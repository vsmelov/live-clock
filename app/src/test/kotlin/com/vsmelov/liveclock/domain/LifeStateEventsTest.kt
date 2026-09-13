package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean). Комментарии и сообщения
 * ассертов при этом остаются русскими.
 */
class LifeStateEventsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun instant(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `empty state does not move the expected instant`() {
        assertEquals(0, LifeState().totalDeltaMinutes)
    }

    @Test
    fun `deltas add up across the whole log`() {
        val at = instant("2026-09-13T10:00:00")
        val state = LifeState()
            .plusEvent(LifeEvent.now(EventType.SMOKE, at))
            .plusEvent(LifeEvent.now(EventType.DRINK, at.plusSeconds(1)))
            .plusEvent(LifeEvent.now(EventType.WORKOUT, at.plusSeconds(2)))

        assertEquals(-15 + -30 + 30, state.totalDeltaMinutes)
    }

    @Test
    fun `events stay sorted by time regardless of insertion order`() {
        val noon = instant("2026-09-13T12:00:00")
        val morning = instant("2026-09-13T08:00:00")
        val evening = instant("2026-09-13T20:00:00")

        val state = LifeState()
            .plusEvent(LifeEvent.now(EventType.SMOKE, noon))
            .plusEvent(LifeEvent.now(EventType.REST, evening))
            .plusEvent(LifeEvent.now(EventType.DRINK, morning))

        assertEquals(listOf(morning, noon, evening), state.events.map { it.at })
    }

    @Test
    fun `undo removes the latest event by time`() {
        val morning = instant("2026-09-13T08:00:00")
        val evening = instant("2026-09-13T20:00:00")

        // записано в обратном порядке: вечернее добавлено первым
        val state = LifeState()
            .plusEvent(LifeEvent.now(EventType.WORKOUT, evening))
            .plusEvent(LifeEvent.now(EventType.SMOKE, morning))

        val undone = state.withoutLastEvent()

        assertEquals(1, undone.events.size)
        assertEquals(morning, undone.events.single().at)
        assertEquals(EventType.SMOKE, undone.events.single().type)
    }

    @Test
    fun `undo on an empty log is a no-op`() {
        val empty = LifeState()
        assertSame(empty, empty.withoutLastEvent())
        assertTrue(empty.withoutLastEvent().events.isEmpty())
    }

    @Test
    fun `last event is exposed and null on a clean state`() {
        assertNull(LifeState().lastEvent)

        val at = instant("2026-09-13T12:00:00")
        val state = LifeState().plusEvent(LifeEvent.now(EventType.REST, at))
        assertEquals(EventType.REST, state.lastEvent?.type)
    }

    @Test
    fun `undo restores the previous delta sum`() {
        val at = instant("2026-09-13T12:00:00")
        val before = LifeState().plusEvent(LifeEvent.now(EventType.WORKOUT, at))
        val after = before
            .plusEvent(LifeEvent.now(EventType.SMOKE, at.plusSeconds(60)))
            .withoutLastEvent()

        assertEquals(before.totalDeltaMinutes, after.totalDeltaMinutes)
    }

    @Test
    fun `today log filters by local date and returns newest first`() {
        val yesterday = instant("2026-09-12T23:30:00")
        val morning = instant("2026-09-13T08:00:00")
        val evening = instant("2026-09-13T20:00:00")
        val tomorrow = instant("2026-09-14T00:30:00")

        val state = LifeState()
            .plusEvent(LifeEvent.now(EventType.SMOKE, yesterday))
            .plusEvent(LifeEvent.now(EventType.DRINK, morning))
            .plusEvent(LifeEvent.now(EventType.REST, evening))
            .plusEvent(LifeEvent.now(EventType.WORKOUT, tomorrow))

        val today = state.eventsOn(LocalDate.of(2026, 9, 13), zone)

        assertEquals(listOf(evening, morning), today.map { it.at })
    }

    @Test
    fun `editing a coefficient does not rewrite the recorded log`() {
        val at = instant("2026-09-13T12:00:00")
        // событие, записанное когда сигарета стоила вдвое дороже
        val historical = LifeEvent(type = EventType.SMOKE, at = at, deltaMinutes = -30)
        val state = LifeState().plusEvent(historical)

        assertEquals(-30, state.totalDeltaMinutes)
        assertEquals(-15, EventType.SMOKE.deltaMinutes)
    }

    @Test
    fun `unsynced events are selected by timestamp`() {
        val first = instant("2026-09-13T08:00:00")
        val second = instant("2026-09-13T12:00:00")
        val third = instant("2026-09-13T20:00:00")

        val state = LifeState()
            .plusEvent(LifeEvent.now(EventType.SMOKE, first))
            .plusEvent(LifeEvent.now(EventType.DRINK, second))
            .plusEvent(LifeEvent.now(EventType.REST, third))

        assertEquals(listOf(third), state.eventsAfter(second).map { it.at })
        assertEquals(3, state.eventsAfter(null).size)
        assertTrue(state.eventsAfter(third).isEmpty())
    }
}
