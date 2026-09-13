package com.vsmelov.liveclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class LifeMathTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun instant(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `whole expectancy lands exactly on the birthday anniversary`() {
        val state = LifeState(
            birthDate = LocalDate.of(1994, 2, 4),
            baseExpectancyYears = 80.0,
        )

        assertEquals(
            instant("2074-02-04T00:00:00"),
            LifeMath.expectedDeathInstant(state, zone),
        )
    }

    @Test
    fun `fractional expectancy adds an average year length`() {
        val state = LifeState(baseExpectancyYears = 80.5)

        val whole = LifeMath.expectedDeathInstant(state.copy(baseExpectancyYears = 80.0), zone)
        val withHalf = LifeMath.expectedDeathInstant(state, zone)

        // Checks the meaning (half a year was added), not the rounding mode:
        // SECONDS_PER_YEAR is 31556951.999999996 as a double, and comparing to
        // the second would pin the implementation rather than the behaviour.
        assertEquals(
            LifeMath.SECONDS_PER_YEAR / 2,
            Duration.between(whole, withHalf).seconds.toDouble(),
            1.0,
        )
    }

    @Test
    fun `negative expectancy is clamped to zero`() {
        val birth = LocalDate.of(1994, 2, 4)
        val state = LifeState(birthDate = birth, baseExpectancyYears = -10.0)

        assertEquals(
            instant("1994-02-04T00:00:00"),
            LifeMath.expectedDeathInstant(state, zone),
        )
    }

    @Test
    fun `a cigarette moves the expected instant fifteen minutes earlier`() {
        val at = instant("2026-09-13T12:00:00")
        val clean = LifeState()
        val smoked = clean.plusEvent(LifeEvent(EventType.SMOKE, at, Coefficients.SMOKE))

        val shift = Duration.between(
            LifeMath.expectedDeathInstant(smoked, zone),
            LifeMath.expectedDeathInstant(clean, zone),
        )
        assertEquals(Duration.ofMinutes(15), shift)
    }

    @Test
    fun `opposite events cancel each other out`() {
        val at = instant("2026-09-13T12:00:00")
        val clean = LifeState()
        val mixed = clean
            .plusEvent(LifeEvent(EventType.SMOKE, at, -15))
            .plusEvent(LifeEvent(EventType.SMOKE, at.plusSeconds(1), -15))
            .plusEvent(LifeEvent(EventType.WORKOUT, at.plusSeconds(2), 30))

        assertEquals(
            LifeMath.expectedDeathInstant(clean, zone),
            LifeMath.expectedDeathInstant(mixed, zone),
        )
    }

    @Test
    fun `remaining equals the distance to the expected instant`() {
        val state = LifeState(birthDate = LocalDate.of(1994, 2, 4), baseExpectancyYears = 80.0)
        val now = instant("2026-09-13T00:00:00")

        val remaining = LifeMath.remaining(state, now, zone)

        assertEquals(Duration.between(now, instant("2074-02-04T00:00:00")), remaining)
        assertTrue(remaining > Duration.ZERO)
    }

    @Test
    fun `remaining years is plausible for the default state`() {
        val now = instant("2026-09-13T00:00:00")
        // from 2026-09 to 2074-02 is roughly 47.4 years
        assertEquals(47.4, LifeMath.remainingYears(LifeState(), now, zone), 0.1)
    }

    @Test
    fun `overdue remaining stays negative instead of clamping to zero`() {
        val state = LifeState(birthDate = LocalDate.of(1900, 1, 1), baseExpectancyYears = 80.0)
        val now = instant("2026-09-13T00:00:00")

        assertTrue(LifeMath.remaining(state, now, zone).isNegative)
        assertTrue(LifeMath.remainingYears(state, now, zone) < 0)
    }

    @Test
    fun `remaining is formatted with four decimal places`() {
        assertEquals("51.2847", LifeMath.formatYears(51.28474))
        assertEquals("0.0000", LifeMath.formatYears(0.0))
        assertEquals("-1.5000", LifeMath.formatYears(-1.5))
    }

    @Test
    fun `formatting ignores the default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru-RU"))
            assertEquals("51.2847", LifeMath.formatYears(51.28474))
            assertEquals("63.874%", LifeMath.formatElapsedPercent(0.63874))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `start of next day is the next local midnight`() {
        assertEquals(
            instant("2026-09-14T00:00:00"),
            LifeMath.startOfNextDay(instant("2026-09-13T21:11:00"), zone),
        )
        assertEquals(
            instant("2026-09-14T00:00:00"),
            LifeMath.startOfNextDay(instant("2026-09-13T00:00:00"), zone),
        )
    }

    @Test
    fun `one microlife is half an hour`() {
        assertEquals(30, Coefficients.MICROLIFE_MINUTES)
        assertEquals(-0.5, EventType.SMOKE.microlives, 1e-9)
        assertEquals(1.0, EventType.WORKOUT.microlives, 1e-9)
    }
}
