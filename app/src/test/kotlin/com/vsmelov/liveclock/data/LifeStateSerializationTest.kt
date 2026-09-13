package com.vsmelov.liveclock.data

import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LifeStateSerializationTest {

    private fun sample(): LifeState = LifeState(
        birthDate = LocalDate.of(1994, 2, 4),
        baseExpectancyYears = 80.5,
        events = listOf(
            LifeEvent(EventType.SMOKE, Instant.ofEpochSecond(1_757_000_000), -15),
            LifeEvent(EventType.WORKOUT, Instant.ofEpochSecond(1_757_100_000), 30),
        ),
    )

    @Test
    fun `state survives a serialization round trip`() {
        val original = sample()
        val json = LifeClockJson.encodeToString(original.toDto())
        assertEquals(original, LifeClockJson.decodeFromString<LifeStateDto>(json).toDomain())
    }

    @Test
    fun `events serialize by stable id rather than enum name`() {
        val json = LifeClockJson.encodeToString(sample().toDto())

        assertTrue(json, json.contains("\"smoke\""))
        assertTrue(json, json.contains("\"workout\""))
        assertTrue("an ordinal must never reach the format", !json.contains("\"type\": 0"))
    }

    @Test
    fun `an unknown event type is skipped and the rest is read`() {
        val json = """
            {
              "schema_version": 1,
              "birth_date": "1994-02-04",
              "base_expectancy_years": 80.0,
              "events": [
                {"type": "smoke", "at_epoch_second": 1757000000, "delta_minutes": -15},
                {"type": "teleportation", "at_epoch_second": 1757000100, "delta_minutes": 999},
                {"type": "rest", "at_epoch_second": 1757000200, "delta_minutes": 15}
              ]
            }
        """.trimIndent()

        val restored = LifeClockJson.decodeFromString<LifeStateDto>(json).toDomain()

        assertEquals(listOf(EventType.SMOKE, EventType.REST), restored.events.map { it.type })
        assertEquals(0, restored.totalDeltaMinutes)
    }

    @Test
    fun `unknown fields from a future schema do not break reading`() {
        val json = """
            {
              "schema_version": 99,
              "birth_date": "1994-02-04",
              "base_expectancy_years": 80.0,
              "events": [],
              "some_future_field": {"nested": true}
            }
        """.trimIndent()

        val restored = LifeClockJson.decodeFromString<LifeStateDto>(json).toDomain()

        assertEquals(LocalDate.of(1994, 2, 4), restored.birthDate)
        assertTrue(restored.events.isEmpty())
    }

    @Test
    fun `events are restored sorted even from shuffled json`() {
        val json = """
            {
              "birth_date": "1994-02-04",
              "base_expectancy_years": 80.0,
              "events": [
                {"type": "rest", "at_epoch_second": 1757000200, "delta_minutes": 15},
                {"type": "smoke", "at_epoch_second": 1757000000, "delta_minutes": -15}
              ]
            }
        """.trimIndent()

        val restored = LifeClockJson.decodeFromString<LifeStateDto>(json).toDomain()

        assertEquals(
            listOf(1_757_000_000L, 1_757_000_200L),
            restored.events.map { it.at.epochSecond },
        )
    }

    @Test
    fun `a historical delta is preserved rather than recomputed`() {
        val historical = LifeState().plusEvent(
            LifeEvent(EventType.SMOKE, Instant.ofEpochSecond(1_757_000_000), deltaMinutes = -60),
        )

        val restored = LifeClockJson
            .decodeFromString<LifeStateDto>(LifeClockJson.encodeToString(historical.toDto()))
            .toDomain()

        assertEquals(-60, restored.totalDeltaMinutes)
    }
}

/** Export and restore — the only thing standing between a lost phone and a lost history. */
class BackupTest {

    private fun sample(): LifeState = LifeState(
        birthDate = LocalDate.of(1994, 2, 4),
        baseExpectancyYears = 80.0,
        events = listOf(
            LifeEvent(EventType.SMOKE, Instant.ofEpochSecond(1_757_000_000), -15),
            LifeEvent(EventType.RED_MEAT, Instant.ofEpochSecond(1_757_100_000), 0),
        ),
    )

    @Test
    fun `a backup round trips without losing anything`() {
        val original = sample()
        assertEquals(original, Backup.decode(Backup.encode(original)))
    }

    @Test
    fun `an exported file is readable by a human`() {
        val text = Backup.encode(sample())
        // Pretty-printed on purpose: this is a file somebody may open and edit.
        assertTrue(text, text.contains("\n"))
        assertTrue(text, text.contains("birth_date"))
    }

    @Test
    fun `a free allowance price of zero survives the round trip`() {
        // A meat serving inside the weekly allowance costs nothing, and that
        // zero has to come back as a zero rather than as the headline figure.
        val restored = Backup.decode(Backup.encode(sample()))
        assertEquals(0, restored?.events?.last()?.deltaMinutes)
    }

    @Test
    fun `garbage returns null instead of throwing at a button press`() {
        assertNull(Backup.decode("this is not json"))
        assertNull(Backup.decode(""))
        assertNull(Backup.decode("{\"birth_date\": 42}"))
    }

    @Test
    fun `the suggested file name carries the date so backups sort`() {
        assertEquals("live-clock-2026-09-20.json", Backup.fileName("2026-09-20"))
    }
}
