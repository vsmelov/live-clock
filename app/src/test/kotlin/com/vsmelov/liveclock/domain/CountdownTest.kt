package com.vsmelov.liveclock.domain

/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean).
 */
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

    /** Состояние, в котором до конца ровно [days] суток и [within] сверху. */
    private fun stateWithRemaining(days: Long, within: Duration, now: Instant): LifeState {
        val death = now.plus(Duration.ofDays(days)).plus(within)
        val birth = LocalDate.ofInstant(death, zone).minusYears(80)
        // Подгоняем ровно: базовая продолжительность целая, разницу добираем событием.
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
    }

    @Test
    fun `default state gives a plausible day count`() {
        val now = instant("2026-09-13T22:03:00")
        // с 13.09.2026 до 04.02.2074 — около 17.3 тысяч суток
        assertEquals(17310L, LifeMath.remainingWholeDays(LifeState(), now, zone))
    }

    @Test
    fun `logging a cigarette shortens the countdown by fifteen minutes`() {
        val now = instant("2026-09-13T22:03:00")
        val before = LifeState()
        val after = before.plusEvent(LifeEvent.now(EventType.SMOKE, now))

        val diff = LifeMath.remaining(before, now, zone).minus(LifeMath.remaining(after, now, zone))
        assertEquals(Duration.ofMinutes(15), diff)
    }
}
