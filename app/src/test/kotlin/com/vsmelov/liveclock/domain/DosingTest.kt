package com.vsmelov.liveclock.domain

/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean).
 */
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DosingTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun at(text: String): Instant =
        LocalDateTime.parse(text).atZone(zone).toInstant()

    /** Записывает [times] событий подряд, считая цену каждого по текущему логу. */
    private fun log(state: LifeState, type: EventType, vararg moments: String): LifeState =
        moments.fold(state) { acc, moment ->
            val instant = at(moment)
            acc.plusEvent(
                LifeEvent(type, instant, deltaMinutes = acc.deltaFor(type, instant, zone)),
            )
        }

    @Test
    fun `meat within the weekly norm is free and only the excess is charged`() {
        var state = LifeState()
        // норма — три порции в неделю
        state = log(state, EventType.RED_MEAT,
            "2026-09-14T12:00:00", "2026-09-16T12:00:00", "2026-09-18T12:00:00")

        assertEquals("три порции за неделю не должны стоить ничего", 0, state.totalDeltaMinutes)

        state = log(state, EventType.RED_MEAT, "2026-09-19T12:00:00")
        assertEquals(Coefficients.RED_MEAT, state.totalDeltaMinutes)
    }

    @Test
    fun `eating meat every few days is never charged`() {
        // Ровно сценарий «ем раз в несколько дней»: две порции в неделю,
        // месяц подряд. Ни одна не должна ничего стоить.
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
    fun `the weekly norm resets on monday`() {
        var state = LifeState()
        // воскресенье — норма выбрана
        state = log(state, EventType.RED_MEAT,
            "2026-09-14T12:00:00", "2026-09-15T12:00:00", "2026-09-20T23:00:00")
        assertEquals(0, state.remainingInNorm(EventType.RED_MEAT, at("2026-09-20T23:30:00"), zone))

        // понедельник — снова полная норма
        assertEquals(3, state.remainingInNorm(EventType.RED_MEAT, at("2026-09-21T00:30:00"), zone))
        assertEquals(0, state.deltaFor(EventType.RED_MEAT, at("2026-09-21T00:30:00"), zone))
    }

    @Test
    fun `the first drink of the day helps and the next ones hurt`() {
        // Ровно то, что написано в источнике: первая доза за день в плюс,
        // последующие в минус.
        var state = LifeState()
        state = log(state, EventType.DRINK, "2026-09-14T20:00:00")
        assertTrue("первая доза за день должна быть в плюс", state.totalDeltaMinutes > 0)

        val afterFirst = state.totalDeltaMinutes
        state = log(state, EventType.DRINK, "2026-09-14T21:00:00")
        assertEquals(afterFirst + Coefficients.DRINK, state.totalDeltaMinutes)
    }

    @Test
    fun `the daily norm resets the next morning`() {
        var state = LifeState()
        state = log(state, EventType.DRINK, "2026-09-14T23:00:00")
        assertEquals(0, state.remainingInNorm(EventType.DRINK, at("2026-09-14T23:30:00"), zone))
        assertEquals(1, state.remainingInNorm(EventType.DRINK, at("2026-09-15T00:30:00"), zone))
    }

    @Test
    fun `a benefit stops growing past its norm instead of going negative`() {
        var state = LifeState()
        // кофе: три чашки в день дают плюс, четвёртая — уже ноль, но не минус
        listOf("2026-09-14T08:00:00", "2026-09-14T11:00:00", "2026-09-14T13:00:00")
            .forEach { moment -> state = log(state, EventType.COFFEE, moment) }
        val afterThree = state.totalDeltaMinutes
        assertEquals(3 * Coefficients.COFFEE, afterThree)

        state = log(state, EventType.COFFEE, "2026-09-14T15:00:00")
        assertEquals("четвёртая чашка не должна ни давать, ни отнимать", afterThree, state.totalDeltaMinutes)
    }

    @Test
    fun `types without a norm always cost the same`() {
        val withoutNorm = EventType.entries.filter { it.dosing == null }
        assertTrue(withoutNorm.isNotEmpty())
        withoutNorm.forEach { type ->
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
        // Безопасной дозы у сигарет нет, и норма тут была бы враньём.
        assertEquals(null, EventType.SMOKE.dosing)
    }

    @Test
    fun `the headline value always matches one side of the norm`() {
        // Иначе на кнопке была бы цифра, которой не бывает ни при каких условиях.
        EventType.entries.mapNotNull { type -> type.dosing?.let { type to it } }
            .forEach { (type, dosing) ->
                assertTrue(
                    "$type: номинал ${type.deltaMinutes} не совпадает ни с одной стороной нормы",
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

        val prices = state.events.map { it.deltaMinutes }
        assertEquals(listOf(0, 0, 0, Coefficients.RED_MEAT), prices)
    }

    @Test
    fun `a negative norm is rejected`() {
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
        assertTrue("воскресенье — та же ISO-неделя", DosingPeriod.WEEK.isSamePeriod(monday, sunday, zone))
        assertTrue(!DosingPeriod.WEEK.isSamePeriod(monday, nextMonday, zone))
    }
}
