package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CountdownTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun instant(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    /** A state where exactly [days] whole days plus [within] remain. */
    private fun stateWithRemaining(days: Long, within: Duration, now: Instant): LifeState {
        val death = now.plus(Duration.ofDays(days)).plus(within)
        val birth = LocalDate.ofInstant(death, zone).minusYears(80)
        val base = LifeState(birthDate = birth, baseExpectancyYears = 80.0)
        val drift = Duration.between(LifeMath.expectedDeathInstant(base, zone), death)
        return base.plusEvent(
            LifeEvent(EventType.REST, now, deltaMinutes = drift.toMinutes().toInt()),
        )
    }

    @Test
    fun `whole days and within-day remainder add back up to the full remaining`() {
        val now = instant("2026-09-13T22:03:00")
        val state = stateWithRemaining(17310, Duration.ofHours(1).plusMinutes(56), now)

        val days = LifeMath.remainingWholeDays(state, now, zone)
        val within = LifeMath.remainingWithinDay(state, now, zone)

        assertEquals(
            LifeMath.remaining(state, now, zone).toMinutes(),
            Duration.ofDays(days).plus(within).toMinutes(),
        )
    }

    @Test
    fun `within-day remainder never reaches a full day`() {
        val now = instant("2026-09-13T22:03:00")
        listOf(
            Duration.ZERO,
            Duration.ofSeconds(1),
            Duration.ofHours(23).plusMinutes(59).plusSeconds(59),
        ).forEach { within ->
            val state = stateWithRemaining(100, within, now)
            val actual = LifeMath.remainingWithinDay(state, now, zone)
            assertTrue("$within -> $actual", actual < Duration.ofDays(1))
            assertTrue("$within -> $actual", !actual.isNegative)
        }
    }

    @Test
    fun `overdue life shows zero instead of a negative countdown`() {
        val state = LifeState(birthDate = LocalDate.of(1900, 1, 1), baseExpectancyYears = 80.0)
        val now = instant("2026-09-13T22:03:00")

        assertEquals(0L, LifeMath.remainingWholeDays(state, now, zone))
        assertEquals(Duration.ZERO, LifeMath.remainingWithinDay(state, now, zone))
        assertEquals("0d 0:00:00", LifeMath.formatCountdown(state, now, zone))
    }

    @Test
    fun `default state gives a plausible day count`() {
        val now = instant("2026-09-13T22:03:00")
        assertEquals(17310L, LifeMath.remainingWholeDays(LifeState(), now, zone))
    }

    @Test
    fun `countdown is printed as days then a padded clock`() {
        val now = instant("2026-09-13T22:03:00")
        val text = LifeMath.formatCountdown(LifeState(), now, zone)

        assertTrue(text, Regex("""^\d+d \d{1,2}:\d{2}:\d{2}$""").matches(text))
        assertTrue(text, text.startsWith("17310d "))
    }

    @Test
    fun `chronometer format leaves a single placeholder for the ticking part`() {
        val format = LifeMath.chronometerFormat(17310)

        assertEquals("17310d %s", format)
        // The Chronometer substitutes through String.format, so no stray specifiers.
        assertEquals(1, Regex("%").findAll(format).count())
        assertEquals("17310d 1:56:26", format.format("1:56:26"))
    }
}
