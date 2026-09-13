package com.vsmelov.liveclock.domain

import androidx.annotation.StringRes
import com.vsmelov.liveclock.R
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields

/**
 * An allowance per period: how much fits before a penalty, and what going over costs.
 *
 * This exists because the epidemiology almost everywhere measures an EXTRA
 * serving on top of habit, not every serving. Meat twice a week is the ordinary
 * background the baseline forecast was computed from; charging for it is both
 * wrong on the merits and useless as a signal. The penalty belongs on the excess.
 *
 * The same mechanism repairs alcohol: in the source the first drink of the day
 * goes up and only the following ones go down.
 */
data class Dosing(
    val period: DosingPeriod,
    /** How many events fit inside the allowance per period. */
    val normal: Int,
    /** What an event costs while still inside the allowance. */
    val withinNormalMinutes: Int,
    /** What each event beyond the allowance costs. */
    val beyondNormalMinutes: Int,
) {
    init {
        require(normal >= 0) { "an allowance cannot be negative" }
    }
}

enum class DosingPeriod(@StringRes val labelRes: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    ;

    /** Whether two moments fall inside the same period in [zone]. */
    fun isSamePeriod(first: Instant, second: Instant, zone: ZoneId): Boolean {
        val a = first.atZone(zone).toLocalDate()
        val b = second.atZone(zone).toLocalDate()
        return when (this) {
            DAY -> a == b
            // ISO calendar week, starting Monday: "per week" is what a person
            // means by that, not a rolling seven days.
            WEEK -> {
                val week = WeekFields.ISO.weekOfWeekBasedYear()
                val year = WeekFields.ISO.weekBasedYear()
                a.get(week) == b.get(week) && a.get(year) == b.get(year)
            }
        }
    }
}
