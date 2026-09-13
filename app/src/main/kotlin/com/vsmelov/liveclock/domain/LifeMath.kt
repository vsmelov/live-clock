package com.vsmelov.liveclock.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * All of the life-remaining arithmetic. Pure functions with no state and no
 * android.* imports, so plain JVM tests cover them.
 *
 * The formula: the expected moment of death is the date of birth plus the base
 * life expectancy plus the sum of every delta in the log. What remains is the
 * distance from that moment to now.
 */
object LifeMath {

    /**
     * The mean length of a Gregorian year in days.
     *
     * Whole years are added by the calendar (via [java.time.LocalDate.plusYears]),
     * so leap years are handled exactly. This constant is only needed for the
     * fractional part of a life expectancy: 80.5 years means 80 whole calendar
     * years plus half a year measured by the mean year length.
     */
    const val DAYS_PER_YEAR: Double = 365.2425

    /** Seconds in a mean year — the basis for expressing the remainder in years. */
    const val SECONDS_PER_YEAR: Double = DAYS_PER_YEAR * 24.0 * 60.0 * 60.0

    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val NANOS_PER_SECOND = 1_000_000_000.0

    /**
     * The expected moment of death, accounting for the whole log.
     *
     * Counted from midnight on the date of birth in [zone]. A negative base life
     * expectancy is meaningless and is clamped to zero.
     */
    fun expectedDeathInstant(state: LifeState, zone: ZoneId): Instant {
        val expectancy = state.baseExpectancyYears.coerceAtLeast(0.0)
        val wholeYears = floor(expectancy).toLong()
        val fractionOfYear = expectancy - wholeYears

        val anniversary = state.birthDate
            .plusYears(wholeYears)
            .atStartOfDay(zone)
            .toInstant()

        return anniversary
            .plusSeconds((fractionOfYear * SECONDS_PER_YEAR).roundToLong())
            .plusSeconds(state.totalDeltaMinutes.toLong() * SECONDS_PER_MINUTE)
    }

    /**
     * What remains as of [now].
     *
     * Signed on purpose: if the expected moment is already behind, this returns a
     * negative duration. Pretending it is zero would be a lie.
     */
    fun remaining(state: LifeState, now: Instant, zone: ZoneId): Duration =
        Duration.between(now, expectedDeathInstant(state, zone))

    /** The same remainder expressed in years. Signed, see [remaining]. */
    fun remainingYears(state: LifeState, now: Instant, zone: ZoneId): Double {
        val left = remaining(state, now, zone)
        return (left.seconds + left.nano / NANOS_PER_SECOND) / SECONDS_PER_YEAR
    }

    /**
     * Whole days remaining.
     *
     * The widget shows days as a separate number and lets the Chronometer tick
     * only the within-day remainder — see [remainingWithinDay]. A Chronometer can
     * only format "H:MM:SS", so 17,310 days would come out as 415,440 hours.
     */
    fun remainingWholeDays(state: LifeState, now: Instant, zone: ZoneId): Long {
        val left = remaining(state, now, zone)
        return if (left.isNegative) 0 else left.toDays()
    }

    /**
     * The remainder with whole days taken out — what ticks by the second on the
     * widget. Always between zero and one day.
     */
    fun remainingWithinDay(state: LifeState, now: Instant, zone: ZoneId): Duration {
        val left = remaining(state, now, zone)
        if (left.isNegative) return Duration.ZERO
        return left.minusDays(left.toDays())
    }

    /**
     * The fraction of life already lived: 0.0 at birth, 1.0 at the expected moment.
     *
     * Measured from midnight on the date of birth to the expected moment, so it
     * accounts for the whole log: every cigarette moves the denominator as well
     * as the remainder.
     */
    fun elapsedFraction(state: LifeState, now: Instant, zone: ZoneId): Double {
        val birth = state.birthDate.atStartOfDay(zone).toInstant()
        val death = expectedDeathInstant(state, zone)
        val total = Duration.between(birth, death).seconds
        if (total <= 0) return 1.0
        val lived = Duration.between(birth, now).seconds
        return (lived.toDouble() / total).coerceIn(0.0, 1.0)
    }

    /** The fraction lived as a percentage with three decimals: "63.874%". */
    fun formatElapsedPercent(fraction: Double): String =
        "%.3f%%".format(Locale.US, fraction * 100.0)

    /**
     * The remainder in years with four decimals. The separator is a dot
     * regardless of the device locale.
     */
    fun formatYears(years: Double): String = "%.4f".format(Locale.US, years)

    /** Ready-made string for the widget's secondary line. */
    fun formatRemainingYears(state: LifeState, now: Instant, zone: ZoneId): String =
        formatYears(remainingYears(state, now, zone))

    /**
     * The Chronometer format string carrying the day count: "17310d %s".
     *
     * The days are ours; the "H:MM:SS" part is drawn and ticked by the
     * Chronometer itself. The string goes through String.format inside the
     * Chronometer, which is why nothing here may contain a stray percent sign.
     */
    fun chronometerFormat(days: Long): String = "${days}d %s"

    /**
     * The remainder on one line with seconds: "17310d 23:21:45".
     *
     * For the Activity, where seconds can honestly be redrawn every second. The
     * widget assembles the same look from [chronometerFormat] and a Chronometer,
     * because nothing will wake us once a second out there.
     */
    fun formatCountdown(state: LifeState, now: Instant, zone: ZoneId): String {
        val days = remainingWholeDays(state, now, zone)
        val within = remainingWithinDay(state, now, zone)
        return "%dd %d:%02d:%02d".format(
            Locale.US,
            days,
            within.toHours(),
            within.toMinutes() % MINUTES_PER_HOUR,
            within.seconds % SECONDS_PER_MINUTE,
        )
    }

    /**
     * The start of the next calendar day in [zone].
     *
     * [java.time.LocalDate.atStartOfDay] handles clock changes correctly: if
     * midnight does not exist on that day, it returns the first moment that does.
     */
    fun startOfNextDay(now: Instant, zone: ZoneId): Instant =
        now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
}
