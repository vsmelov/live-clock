package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Streaks and the weekly summary — the parts meant to grow a habit rather than
 * just price a mistake.
 */
class StreakTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private val today: LocalDate = LocalDate.of(2026, 9, 20)

    private fun at(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun log(vararg entries: Pair<EventType, String>): LifeState =
        entries.fold(LifeState()) { state, (type, moment) ->
            state.plusEvent(LifeEvent(type, at(moment), type.deltaMinutes))
        }

    @Test
    fun `a type never logged produces no streak`() {
        // "Never smoked" is an infinite streak and says nothing.
        assertTrue(LifeState().streaks(today, zone).isEmpty())
    }

    @Test
    fun `days since the last costly action become a clean streak`() {
        val state = log(EventType.SMOKE to "2026-09-15T12:00:00")

        val streak = state.streaks(today, zone).single { it.type == EventType.SMOKE }

        assertEquals(5, streak.days)
        assertTrue("a costly action gives a clean streak", streak.clean)
    }

    @Test
    fun `a costly action logged today gives no streak`() {
        val state = log(EventType.SMOKE to "2026-09-20T09:00:00")
        assertTrue(state.streaks(today, zone).none { it.type == EventType.SMOKE })
    }

    @Test
    fun `consecutive days of a beneficial action build a streak`() {
        val state = log(
            EventType.WORKOUT to "2026-09-17T09:00:00",
            EventType.WORKOUT to "2026-09-18T09:00:00",
            EventType.WORKOUT to "2026-09-19T09:00:00",
            EventType.WORKOUT to "2026-09-20T09:00:00",
        )

        val streak = state.streaks(today, zone).single { it.type == EventType.WORKOUT }

        assertEquals(4, streak.days)
        assertTrue("a beneficial action is not a clean streak", !streak.clean)
    }

    @Test
    fun `a beneficial streak survives a day that has not happened yet`() {
        // Ending yesterday is legitimate: today may simply not have happened.
        // Counting from today would reset every streak each morning.
        val state = log(
            EventType.WORKOUT to "2026-09-17T09:00:00",
            EventType.WORKOUT to "2026-09-18T09:00:00",
            EventType.WORKOUT to "2026-09-19T09:00:00",
        )

        assertEquals(3, state.streaks(today, zone).single().days)
    }

    @Test
    fun `a gap breaks a beneficial streak`() {
        val state = log(
            EventType.WORKOUT to "2026-09-16T09:00:00",
            // 17 September missing
            EventType.WORKOUT to "2026-09-18T09:00:00",
            EventType.WORKOUT to "2026-09-19T09:00:00",
        )

        assertEquals(2, state.streaks(today, zone).single().days)
    }

    @Test
    fun `streaks shorter than the minimum are not shown`() {
        val state = log(EventType.WORKOUT to "2026-09-19T09:00:00")
        assertTrue(state.streaks(today, zone).isEmpty())
    }

    @Test
    fun `streaks come back longest first`() {
        val state = log(
            EventType.SMOKE to "2026-09-10T12:00:00",
            EventType.DRINK to "2026-09-17T12:00:00",
        )

        val days = state.streaks(today, zone).map { it.days }
        assertEquals(days.sortedDescending(), days)
    }

    @Test
    fun `week summary counts only the iso week of the reference moment`() {
        val state = log(
            EventType.SMOKE to "2026-09-13T12:00:00", // previous ISO week (Sunday)
            EventType.SMOKE to "2026-09-14T12:00:00", // Monday, this week
            EventType.WORKOUT to "2026-09-16T12:00:00",
        )

        val week = state.weekSummary(at("2026-09-20T12:00:00"), zone)

        assertEquals(2, week.eventCount)
        assertEquals(Coefficients.SMOKE + Coefficients.WORKOUT, week.totalMinutes)
    }

    @Test
    fun `week summary names the biggest gain and the biggest loss`() {
        val state = log(
            EventType.SMOKE to "2026-09-14T12:00:00",
            EventType.SMOKE to "2026-09-15T12:00:00",
            EventType.WORKOUT to "2026-09-16T12:00:00",
        )

        val week = state.weekSummary(at("2026-09-20T12:00:00"), zone)

        assertEquals(EventType.WORKOUT, week.best?.first)
        assertEquals(EventType.SMOKE, week.worst?.first)
        assertEquals(2 * Coefficients.SMOKE, week.worst?.second)
    }

    @Test
    fun `an empty week reports itself as empty`() {
        val week = LifeState().weekSummary(at("2026-09-20T12:00:00"), zone)
        assertTrue(week.isEmpty)
        assertEquals(0, week.totalMinutes)
    }
}
