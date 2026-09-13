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
    fun `целая продолжительность даёт ровно годовщину рождения`() {
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
    fun `дробная часть добавляется по средней длине года`() {
        val state = LifeState(baseExpectancyYears = 80.5)

        val whole = LifeMath.expectedDeathInstant(state.copy(baseExpectancyYears = 80.0), zone)
        val withHalf = LifeMath.expectedDeathInstant(state, zone)

        // Проверяем смысл (добавилось полгода), а не способ округления:
        // SECONDS_PER_YEAR в double — 31556951.999999996, и сравнение с точностью
        // до секунды зафиксировало бы реализацию, а не поведение.
        assertEquals(
            LifeMath.SECONDS_PER_YEAR / 2,
            Duration.between(whole, withHalf).seconds.toDouble(),
            1.0,
        )
    }

    @Test
    fun `отрицательная продолжительность поджимается к нулю`() {
        val birth = LocalDate.of(1994, 2, 4)
        val state = LifeState(birthDate = birth, baseExpectancyYears = -10.0)

        assertEquals(
            instant("1994-02-04T00:00:00"),
            LifeMath.expectedDeathInstant(state, zone),
        )
    }

    @Test
    fun `сигарета сдвигает ожидаемый момент на пятнадцать минут назад`() {
        val clean = LifeState()
        val smoked = clean.plusEvent(
            LifeEvent.now(EventType.SMOKE, instant("2026-09-13T12:00:00")),
        )

        val shift = Duration.between(
            LifeMath.expectedDeathInstant(smoked, zone),
            LifeMath.expectedDeathInstant(clean, zone),
        )
        assertEquals(Duration.ofMinutes(15), shift)
    }

    @Test
    fun `тренировка сдвигает ожидаемый момент на полчаса вперёд`() {
        val clean = LifeState()
        val trained = clean.plusEvent(
            LifeEvent.now(EventType.WORKOUT, instant("2026-09-13T12:00:00")),
        )

        val shift = Duration.between(
            LifeMath.expectedDeathInstant(clean, zone),
            LifeMath.expectedDeathInstant(trained, zone),
        )
        assertEquals(Duration.ofMinutes(30), shift)
    }

    @Test
    fun `разнонаправленные события гасят друг друга`() {
        val at = instant("2026-09-13T12:00:00")
        val clean = LifeState()
        val mixed = clean
            .plusEvent(LifeEvent.now(EventType.SMOKE, at))
            .plusEvent(LifeEvent.now(EventType.SMOKE, at.plusSeconds(1)))
            .plusEvent(LifeEvent.now(EventType.WORKOUT, at.plusSeconds(2)))

        assertEquals(
            LifeMath.expectedDeathInstant(clean, zone),
            LifeMath.expectedDeathInstant(mixed, zone),
        )
    }

    @Test
    fun `остаток равен расстоянию до ожидаемого момента`() {
        val state = LifeState(birthDate = LocalDate.of(1994, 2, 4), baseExpectancyYears = 80.0)
        val now = instant("2026-09-13T00:00:00")

        val remaining = LifeMath.remaining(state, now, zone)

        assertEquals(
            Duration.between(now, instant("2074-02-04T00:00:00")),
            remaining,
        )
        assertTrue(remaining > Duration.ZERO)
    }

    @Test
    fun `остаток в годах правдоподобен для дефолтного состояния`() {
        val state = LifeState()
        val now = instant("2026-09-13T00:00:00")

        val years = LifeMath.remainingYears(state, now, zone)

        // с 2026-09 до 2074-02 — примерно 47.4 года
        assertEquals(47.4, years, 0.1)
    }

    @Test
    fun `просроченный остаток отрицателен, а не обнуляется`() {
        val state = LifeState(birthDate = LocalDate.of(1900, 1, 1), baseExpectancyYears = 80.0)
        val now = instant("2026-09-13T00:00:00")

        assertTrue(LifeMath.remaining(state, now, zone).isNegative)
        assertTrue(LifeMath.remainingYears(state, now, zone) < 0)
    }

    @Test
    fun `формат остатка — четыре знака после точки`() {
        assertEquals("51.2847", LifeMath.formatYears(51.28474))
        assertEquals("51.2847", LifeMath.formatYears(51.284749))
        assertEquals("0.0000", LifeMath.formatYears(0.0))
        assertEquals("-1.5000", LifeMath.formatYears(-1.5))
    }

    @Test
    fun `формат не зависит от локали по умолчанию`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru-RU"))
            assertEquals("51.2847", LifeMath.formatYears(51.28474))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `начало следующих суток — ближайшая локальная полночь`() {
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
    fun `микрожизнь равна получасу`() {
        assertEquals(30, Coefficients.MICROLIFE_MINUTES)
        assertEquals(-0.5, EventType.SMOKE.microlives, 1e-9)
        assertEquals(1.0, EventType.WORKOUT.microlives, 1e-9)
    }
}
