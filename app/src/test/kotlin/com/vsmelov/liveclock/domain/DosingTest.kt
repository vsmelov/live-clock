package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DosingTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun at(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    /** Records events in order, pricing each one against the log as it stands. */
    private fun log(state: LifeState, type: EventType, vararg moments: String): LifeState =
        moments.fold(state) { acc, moment ->
            val instant = at(moment)
            acc.plusEvent(LifeEvent(type, instant, deltaMinutes = acc.deltaFor(type, instant, zone)))
        }

    @Test
    fun `meat within the weekly allowance is free and only the excess is charged`() {
        var state = LifeState()
        state = log(state, EventType.RED_MEAT,
            "2026-09-14T12:00:00", "2026-09-16T12:00:00", "2026-09-18T12:00:00")

        assertEquals("three servings a week should cost nothing", 0, state.totalDeltaMinutes)

        state = log(state, EventType.RED_MEAT, "2026-09-19T12:00:00")
        assertEquals(Coefficients.RED_MEAT, state.totalDeltaMinutes)
    }

    @Test
    fun `eating meat every few days is never charged`() {
        // Exactly the "I eat it every few days" case: two servings a week for a
        // month. Not one of them should cost anything.
        var state = LifeState()
        listOf(
            "2026-09-14T12:00:00", "2026-09-17T12:00:00",
            "2026-09-21T12:00:00", "2026-09-24T12:00:00",
            "2026-09-28T12:00:00", "2026-10-01T12:00:00",
        ).forEach { state = log(state, EventType.RED_MEAT, it) }

        assertEquals(0, state.totalDeltaMinutes)
        assertEquals(6, state.events.size)
    }

    @Test
    fun `the weekly allowance resets on monday`() {
        var state = LifeState()
        state = log(state, EventType.RED_MEAT,
            "2026-09-14T12:00:00", "2026-09-15T12:00:00", "2026-09-20T23:00:00")
        assertEquals(0, state.remainingInNorm(EventType.RED_MEAT, at("2026-09-20T23:30:00"), zone))

        assertEquals(3, state.remainingInNorm(EventType.RED_MEAT, at("2026-09-21T00:30:00"), zone))
        assertEquals(0, state.deltaFor(EventType.RED_MEAT, at("2026-09-21T00:30:00"), zone))
    }

    @Test
    fun `the first drink of the day helps and the next ones hurt`() {
        // Exactly what the source says: the first drink of the day goes up, the
        // following ones go down.
        var state = LifeState()
        state = log(state, EventType.DRINK, "2026-09-14T20:00:00")
        assertTrue("the first drink of the day should be positive", state.totalDeltaMinutes > 0)

        val afterFirst = state.totalDeltaMinutes
        state = log(state, EventType.DRINK, "2026-09-14T21:00:00")
        assertEquals(afterFirst + Coefficients.DRINK, state.totalDeltaMinutes)
    }

    @Test
    fun `the daily allowance resets the next morning`() {
        var state = LifeState()
        state = log(state, EventType.DRINK, "2026-09-14T23:00:00")
        assertEquals(0, state.remainingInNorm(EventType.DRINK, at("2026-09-14T23:30:00"), zone))
        assertEquals(1, state.remainingInNorm(EventType.DRINK, at("2026-09-15T00:30:00"), zone))
    }

    @Test
    fun `a benefit stops growing past its allowance instead of going negative`() {
        var state = LifeState()
        listOf("2026-09-14T08:00:00", "2026-09-14T11:00:00", "2026-09-14T13:00:00")
            .forEach { moment -> state = log(state, EventType.COFFEE, moment) }
        val afterThree = state.totalDeltaMinutes
        assertEquals(3 * Coefficients.COFFEE, afterThree)

        state = log(state, EventType.COFFEE, "2026-09-14T15:00:00")
        assertEquals("a fourth cup should neither give nor take", afterThree, state.totalDeltaMinutes)
    }

    @Test
    fun `types without an allowance always cost the same`() {
        val withoutAllowance = EventType.entries.filter { it.dosing == null }
        assertTrue(withoutAllowance.isNotEmpty())
        withoutAllowance.forEach { type ->
            var state = LifeState()
            listOf(
                "2026-09-14T10:00:00", "2026-09-14T11:00:00", "2026-09-14T12:00:00",
                "2026-09-14T13:00:00", "2026-09-14T14:00:00",
            ).forEach { moment -> state = log(state, type, moment) }
            assertEquals("$type", 5 * type.deltaMinutes, state.totalDeltaMinutes)
        }
    }

    @Test
    fun `smoking has no free allowance`() {
        // There is no safe dose of cigarettes, and an allowance would be a lie.
        assertNull(EventType.SMOKE.dosing)
    }

    @Test
    fun `the headline value always matches one side of the allowance`() {
        // Otherwise a button would show a figure that never actually applies.
        EventType.entries.mapNotNull { type -> type.dosing?.let { type to it } }
            .forEach { (type, dosing) ->
                assertTrue(
                    "$type: headline ${type.deltaMinutes} matches neither side",
                    type.deltaMinutes == dosing.withinNormalMinutes ||
                        type.deltaMinutes == dosing.beyondNormalMinutes,
                )
            }
    }

    @Test
    fun `a recorded event keeps the price it was charged`() {
        var state = LifeState()
        state = log(state, EventType.RED_MEAT,
            "2026-09-14T12:00:00", "2026-09-15T12:00:00",
            "2026-09-16T12:00:00", "2026-09-17T12:00:00")

        assertEquals(listOf(0, 0, 0, Coefficients.RED_MEAT), state.events.map { it.deltaMinutes })
    }

    @Test
    fun `a negative allowance is rejected`() {
        val error = runCatching {
            Dosing(DosingPeriod.DAY, normal = -1, withinNormalMinutes = 0, beyondNormalMinutes = -30)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `day and week periods tell moments apart correctly`() {
        val monday = at("2026-09-14T10:00:00")
        val sameDayLater = at("2026-09-14T23:00:00")
        val sunday = at("2026-09-20T10:00:00")
        val nextMonday = at("2026-09-21T10:00:00")

        assertTrue(DosingPeriod.DAY.isSamePeriod(monday, sameDayLater, zone))
        assertTrue(!DosingPeriod.DAY.isSamePeriod(monday, sunday, zone))
        assertTrue("sunday is the same ISO week", DosingPeriod.WEEK.isSamePeriod(monday, sunday, zone))
        assertTrue(!DosingPeriod.WEEK.isSamePeriod(monday, nextMonday, zone))
    }

    @Test
    fun `every period has a label resource`() {
        DosingPeriod.entries.forEach { period ->
            assertTrue("$period has no label", period.labelRes != 0)
        }
    }
}
