package com.vsmelov.liveclock.data

import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeState
import org.junit.Assert.assertEquals
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
    fun `состояние переживает круг сериализации без потерь`() {
        val original = sample()

        val json = LifeClockJson.encodeToString(original.toDto())
        val restored = LifeClockJson.decodeFromString<LifeStateDto>(json).toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun `события сериализуются по стабильному id, а не по имени enum`() {
        val json = LifeClockJson.encodeToString(sample().toDto())

        assertTrue(json, json.contains("\"type\":\"smoke\""))
        assertTrue(json, json.contains("\"type\":\"workout\""))
        assertTrue("ordinal не должен попадать в формат", !json.contains("\"type\":0"))
    }

    @Test
    fun `неизвестный тип события пропускается, а остальное читается`() {
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
    fun `лишние поля из будущей схемы не ломают чтение`() {
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
    fun `события восстанавливаются отсортированными даже из перемешанного json`() {
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
    fun `историческая дельта сохраняется, а не пересчитывается по текущему коэффициенту`() {
        val historical = LifeState().plusEvent(
            LifeEvent(EventType.SMOKE, Instant.ofEpochSecond(1_757_000_000), deltaMinutes = -60),
        )

        val restored = LifeClockJson
            .decodeFromString<LifeStateDto>(LifeClockJson.encodeToString(historical.toDto()))
            .toDomain()

        assertEquals(-60, restored.totalDeltaMinutes)
    }
}
