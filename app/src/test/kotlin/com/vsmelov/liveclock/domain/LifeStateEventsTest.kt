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

class LifeStateEventsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun instant(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun event(type: EventType, text: String) =
        LifeEvent(type, instant(text), type.deltaMinutes)

    @Test
    fun `empty state does not move the expected instant`() {
        assertEquals(0, LifeState().totalDeltaMinutes)
    }

    @Test
    fun `deltas add up across the whole log`() {
        val state = LifeState()
            .plusEvent(event(EventType.SMOKE, "2026-09-13T10:00:00"))
            .plusEvent(event(EventType.DRINK, "2026-09-13T10:00:01"))
            .plusEvent(event(EventType.WORKOUT, "2026-09-13T10:00:02"))

        assertEquals(
            Coefficients.SMOKE + Coefficients.DRINK + Coefficients.WORKOUT,
            state.totalDeltaMinutes,
        )
    }

    @Test
    fun `events stay sorted by time regardless of insertion order`() {
        val state = LifeState()
            .plusEvent(event(EventType.SMOKE, "2026-09-13T12:00:00"))
            .plusEvent(event(EventType.REST, "2026-09-13T20:00:00"))
            .plusEvent(event(EventType.DRINK, "2026-09-13T08:00:00"))

        assertEquals(
            listOf(
                instant("2026-09-13T08:00:00"),
                instant("2026-09-13T12:00:00"),
                instant("2026-09-13T20:00:00"),
            ),
            state.events.map { it.at },
        )
    }

    @Test
    fun `undo removes the latest event by time`() {
        // Recorded out of order: the evening one was added first.
        val state = LifeState()
            .plusEvent(event(EventType.WORKOUT, "2026-09-13T20:00:00"))
            .plusEvent(event(EventType.SMOKE, "2026-09-13T08:00:00"))

        val undone = state.withoutLastEvent()

        assertEquals(1, undone.events.size)
        assertEquals(instant("2026-09-13T08:00:00"), undone.events.single().at)
        assertEquals(EventType.SMOKE, undone.events.single().type)
    }

    @Test
    fun `undo on an empty log is a no-op`() {
        val empty = LifeState()
        assertSame(empty, empty.withoutLastEvent())
    }

    @Test
    fun `last event is exposed and null on a clean state`() {
        assertNull(LifeState().lastEvent)
        val state = LifeState().plusEvent(event(EventType.REST, "2026-09-13T12:00:00"))
        assertEquals(EventType.REST, state.lastEvent?.type)
    }

    @Test
    fun `today log filters by local date and returns newest first`() {
        val state = LifeState()
            .plusEvent(event(EventType.SMOKE, "2026-09-12T23:30:00"))
            .plusEvent(event(EventType.DRINK, "2026-09-13T08:00:00"))
            .plusEvent(event(EventType.REST, "2026-09-13T20:00:00"))
            .plusEvent(event(EventType.WORKOUT, "2026-09-14T00:30:00"))

        val today = state.eventsOn(LocalDate.of(2026, 9, 13), zone)

        assertEquals(
            listOf(instant("2026-09-13T20:00:00"), instant("2026-09-13T08:00:00")),
            today.map { it.at },
        )
    }

    @Test
    fun `editing a coefficient does not rewrite the recorded log`() {
        // An event recorded back when a cigarette cost twice as much.
        val historical = LifeEvent(EventType.SMOKE, instant("2026-09-13T12:00:00"), -30)
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
            .plusEvent(LifeEvent(EventType.SMOKE, first, -15))
            .plusEvent(LifeEvent(EventType.DRINK, second, -30))
            .plusEvent(LifeEvent(EventType.REST, third, 15))

        assertEquals(listOf(third), state.eventsAfter(second).map { it.at })
        assertEquals(3, state.eventsAfter(null).size)
        assertTrue(state.eventsAfter(third).isEmpty())
    }

    @Test
    fun `usage counts group by type`() {
        val state = LifeState()
            .plusEvent(event(EventType.SMOKE, "2026-09-13T08:00:00"))
            .plusEvent(event(EventType.SMOKE, "2026-09-13T09:00:00"))
            .plusEvent(event(EventType.REST, "2026-09-13T10:00:00"))

        assertEquals(2, state.usageCounts()[EventType.SMOKE])
        assertEquals(1, state.usageCounts()[EventType.REST])
        assertNull(state.usageCounts()[EventType.DRINK])
    }
}
