package com.vsmelov.liveclock.domain

/**
 * Имена тестов латиницей намеренно: из backtick-имени собирается имя .class
 * для лямбд внутри теста, и кириллица в пути ломает сборку под не-UTF-8
 * локалью (POSIX на CI — падает даже clean).
 */
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Пруфы — это обещание пользователю, а не украшение. Тесты держат его так же,
 * как обычные инварианты: ни одного действия без источника, ни одного
 * источника без оговорки.
 */
class EvidenceTest {

    @Test
    fun `every action carries evidence with every field filled in`() {
        EventType.entries.forEach { type ->
            val e = type.evidence
            assertTrue("нет описания воздействия у $type", e.exposure.isNotBlank())
            assertTrue("нет обоснования у $type", e.basis.isNotBlank())
            assertTrue("нет оговорки у $type", e.caveat.isNotBlank())
            assertTrue("нет названия источника у $type", e.sourceTitle.isNotBlank())
            assertTrue("нет ссылки у $type", e.sourceUrl.isNotBlank())
        }
    }

    @Test
    fun `every source link is https`() {
        EventType.entries.forEach { type ->
            assertTrue(
                "ссылка у $type не https: ${type.evidence.sourceUrl}",
                type.evidence.sourceUrl.startsWith("https://"),
            )
        }
    }

    @Test
    fun `a caveat is never a placeholder`() {
        // Оговорка длиннее подписи кнопки — простая защита от «ну, наверное, ок».
        EventType.entries.forEach { type ->
            assertTrue(
                "слишком короткая оговорка у $type",
                type.evidence.caveat.length > type.label.length,
            )
        }
    }

    @Test
    fun `values chosen by hand are marked as chosen, not as research`() {
        val chosen = EventType.entries.filter { it.evidence.confidence == Confidence.CHOSEN }
        assertEquals(
            setOf(EventType.DRINK, EventType.REST, EventType.WORKOUT),
            chosen.toSet(),
        )
    }

    @Test
    fun `an action marked as having no effect really has a zero delta`() {
        EventType.entries
            .filter { it.evidence.confidence == Confidence.NONE }
            .forEach { type ->
                assertEquals("$type помечен как «эффекта нет», но двигает остаток", 0, type.deltaMinutes)
            }
    }

    @Test
    fun `every confidence level has a human readable label`() {
        Confidence.entries.forEach { level ->
            assertTrue("$level без подписи", level.label.isNotBlank())
        }
    }
}

/** Формула пересчёта hazard ratio в минуты. */
class MicrolivesTest {

    @Test
    fun `a neutral hazard ratio moves nothing`() {
        assertEquals(0.0, Microlives.perDay(1.0), 1e-9)
        assertEquals(0, Microlives.minutesPerDay(1.0))
    }

    @Test
    fun `harmful ratios cost life and protective ratios add it`() {
        assertTrue(Microlives.minutesPerDay(1.13) < 0)
        assertTrue(Microlives.minutesPerDay(0.81) > 0)
    }

    @Test
    fun `the formula reproduces the published table rows`() {
        // Автор округляет до целых микрожизней, поэтому сверяем с допуском.
        assertEquals(-1.33, Microlives.perDay(1.13), 0.01)  // красное мясо, в таблице -1
        assertEquals(4.53, Microlives.perDay(0.66), 0.01)   // овощи, в таблице +4
        assertEquals(2.30, Microlives.perDay(0.81), 0.01)   // спорт, в таблице +2
        assertEquals(1.15, Microlives.perDay(0.90), 0.01)   // кофе, в таблице +1
        assertEquals(-0.84, Microlives.perDay(1.08), 0.01)  // телевизор, в таблице -1
    }

    @Test
    fun `values used in the app match their hazard ratios`() {
        assertEquals(Coefficients.LATE_NIGHT, Microlives.minutesPerDay(1.10))
        assertEquals(Coefficients.SLEEP_SHORT, Microlives.minutesPerDay(1.14))
        assertEquals(Coefficients.NATURE, Microlives.minutesPerDay(0.96))
        assertEquals(Coefficients.SOCIAL, Microlives.minutesPerDay(1.0 / 1.29))
    }

    @Test
    fun `the trusted range matches what the authors promise`() {
        assertTrue(Microlives.isTrusted(1.13))
        assertTrue(Microlives.isTrusted(0.81))
        assertTrue("2.17 вне обещанного диапазона", !Microlives.isTrusted(2.17))
        assertTrue("1.34 вне обещанного диапазона", !Microlives.isTrusted(1.34))
    }

    @Test
    fun `a nonpositive hazard ratio is rejected instead of producing nonsense`() {
        listOf(0.0, -1.0).forEach { bad ->
            val error = runCatching { Microlives.perDay(bad) }.exceptionOrNull()
            assertTrue("$bad прошёл без ошибки", error is IllegalArgumentException)
        }
    }
}
