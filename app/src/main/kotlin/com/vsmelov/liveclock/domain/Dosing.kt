package com.vsmelov.liveclock.domain

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields

/**
 * Норма за период: сколько можно без штрафа и чего стоит перебор.
 *
 * Нужна потому, что эпидемиология почти везде меряет ЛИШНЮЮ порцию сверх
 * привычного, а не каждую. Мясо два раза в неделю — это и есть привычный
 * фон, из которого посчитан базовый прогноз; штрафовать за него нечестно
 * и вдобавок бессмысленно как сигнал. Штраф должен включаться на переборе.
 *
 * Та же механика чинит алкоголь: в источнике первая доза за день идёт
 * в плюс, и только следующие в минус.
 */
data class Dosing(
    val period: DosingPeriod,
    /** Сколько событий укладывается в норму за период. */
    val normal: Int,
    /** Что стоит событие в пределах нормы. */
    val withinNormalMinutes: Int,
    /** Что стоит каждое событие сверх нормы. */
    val beyondNormalMinutes: Int,
) {
    init {
        require(normal >= 0) { "норма не может быть отрицательной" }
    }
}

enum class DosingPeriod(val label: String) {
    DAY("за день"),
    WEEK("за неделю"),
    ;

    /** Попадают ли два момента в один и тот же период в зоне [zone]. */
    fun isSamePeriod(first: Instant, second: Instant, zone: ZoneId): Boolean {
        val a = first.atZone(zone).toLocalDate()
        val b = second.atZone(zone).toLocalDate()
        return when (this) {
            DAY -> a == b
            // Календарная неделя по ISO, с понедельника: «за неделю» человек
            // понимает именно так, а не как скользящие семь суток.
            WEEK -> {
                val week = WeekFields.ISO.weekOfWeekBasedYear()
                val year = WeekFields.ISO.weekBasedYear()
                a.get(week) == b.get(week) && a.get(year) == b.get(year)
            }
        }
    }
}
