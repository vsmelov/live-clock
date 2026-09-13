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
import java.util.Locale

class ProgressTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun instant(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `fraction is zero at birth and one at the expected moment`() {
        val state = LifeState()
        val birth = state.birthDate.atStartOfDay(zone).toInstant()
        val death = LifeMath.expectedDeathInstant(state, zone)

        assertEquals(0.0, LifeMath.elapsedFraction(state, birth, zone), 1e-9)
        assertEquals(1.0, LifeMath.elapsedFraction(state, death, zone), 1e-9)
    }

    @Test
    fun `fraction is one half exactly midway`() {
        val state = LifeState()
        val birth = state.birthDate.atStartOfDay(zone).toInstant()
        val death = LifeMath.expectedDeathInstant(state, zone)
        val midpoint = birth.plusSeconds(Duration.between(birth, death).seconds / 2)

        assertEquals(0.5, LifeMath.elapsedFraction(state, midpoint, zone), 1e-6)
    }

    @Test
    fun `fraction never leaves the zero to one range`() {
        val state = LifeState()
        val longBefore = instant("1900-01-01T00:00:00")
        val longAfter = instant("2200-01-01T00:00:00")

        assertEquals(0.0, LifeMath.elapsedFraction(state, longBefore, zone), 1e-9)
        assertEquals(1.0, LifeMath.elapsedFraction(state, longAfter, zone), 1e-9)
    }

    @Test
    fun `a cigarette moves the fraction up, a workout moves it down`() {
        val now = instant("2026-09-13T22:03:00")
        val clean = LifeState()
        val smoked = clean.plusEvent(LifeEvent.now(EventType.SMOKE, now))
        val trained = clean.plusEvent(LifeEvent.now(EventType.WORKOUT, now))

        val base = LifeMath.elapsedFraction(clean, now, zone)

        // Событие двигает и остаток, и знаменатель — доля прожитого растёт.
        assertTrue(LifeMath.elapsedFraction(smoked, now, zone) > base)
        assertTrue(LifeMath.elapsedFraction(trained, now, zone) < base)
    }

    @Test
    fun `percent is printed with three decimals and a dot`() {
        assertEquals("63.874%", LifeMath.formatElapsedPercent(0.63874))
        assertEquals("0.000%", LifeMath.formatElapsedPercent(0.0))
        assertEquals("100.000%", LifeMath.formatElapsedPercent(1.0))
    }

    @Test
    fun `percent format ignores the default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru-RU"))
            assertEquals("63.874%", LifeMath.formatElapsedPercent(0.63874))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `countdown is printed as days then padded clock`() {
        val now = instant("2026-09-13T22:03:00")
        val text = LifeMath.formatCountdown(LifeState(), now, zone)

        assertTrue(text, Regex("""^\d+д \d{1,2}:\d{2}:\d{2}$""").matches(text))
        assertTrue(text, text.startsWith("17310д "))
    }

    @Test
    fun `chronometer format leaves a single placeholder for the ticking part`() {
        val format = LifeMath.chronometerFormat(17310)

        assertEquals("17310д %s", format)
        // Chronometer подставляет через String.format — лишних спецификаторов быть не должно.
        assertEquals(1, Regex("%").findAll(format).count())
        assertEquals("17310д 1:56:26", format.format("1:56:26"))
    }

    @Test
    fun `overdue life reads as a hundred percent and a zero countdown`() {
        val state = LifeState(birthDate = LocalDate.of(1900, 1, 1), baseExpectancyYears = 80.0)
        val now = instant("2026-09-13T22:03:00")

        assertEquals("100.000%", LifeMath.formatElapsedPercent(LifeMath.elapsedFraction(state, now, zone)))
        assertEquals("0д 0:00:00", LifeMath.formatCountdown(state, now, zone))
    }
}
