package com.vsmelov.liveclock.data

import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean). Комментарии и сообщения
 * ассертов при этом остаются русскими.
 */
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
        val restored = LifeClockJson.decodeFromString<LifeStateDto>(json).toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun `events serialize by stable id rather than enum name`() {
        val json = LifeClockJson.encodeToString(sample().toDto())

        assertTrue(json, json.contains("\"type\":\"smoke\""))
        assertTrue(json, json.contains("\"type\":\"workout\""))
        assertTrue("ordinal не должен попадать в формат", !json.contains("\"type\":0"))
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
                {"type": "телепортация", "at_epoch_second": 1757000100, "delta_minutes": 999},
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
              "какое-то_будущее_поле": {"вложенное": true}
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
