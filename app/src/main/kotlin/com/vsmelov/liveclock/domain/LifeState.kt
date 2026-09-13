package com.vsmelov.liveclock.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The whole state of the app. The local DataStore is the source of truth and
 * holds exactly this.
 *
 * [events] is always sorted by [LifeEvent.at] ascending — [plusEvent] and
 * [withoutLastEvent] maintain that.
 */
data class LifeState(
    val birthDate: LocalDate = DEFAULT_BIRTH_DATE,
    val baseExpectancyYears: Double = DEFAULT_BASE_EXPECTANCY_YEARS,
    val events: List<LifeEvent> = emptyList(),
) {

    /** Total correction in minutes across the whole log. */
    val totalDeltaMinutes: Int get() = events.sumOf { it.deltaMinutes }

    /**
     * What an event of [type] recorded at [at] would cost.
     *
     * For types with a [EventType.dosing] allowance the figure depends on how
     * many such events already exist in the period. It is computed at write time
     * and frozen into [LifeEvent.deltaMinutes], so the log stays an honest record
     * of what happened rather than being recomputed after the fact.
     */
    fun deltaFor(type: EventType, at: Instant, zone: ZoneId): Int {
        val dosing = type.dosing ?: return type.deltaMinutes
        return if (countInPeriod(type, at, zone) < dosing.normal) {
            dosing.withinNormalMinutes
        } else {
            dosing.beyondNormalMinutes
        }
    }

    /** How many events of [type] are already recorded in the period containing [at]. */
    fun countInPeriod(type: EventType, at: Instant, zone: ZoneId): Int {
        val dosing = type.dosing ?: return 0
        return events.count { it.type == type && dosing.period.isSamePeriod(it.at, at, zone) }
    }

    /** How much of the allowance is left. Never negative. */
    fun remainingInNorm(type: EventType, at: Instant, zone: ZoneId): Int {
        val dosing = type.dosing ?: return 0
        return (dosing.normal - countInPeriod(type, at, zone)).coerceAtLeast(0)
    }

    /** Adds an event, keeping the list sorted by time. */
    fun plusEvent(event: LifeEvent): LifeState =
        copy(events = (events + event).sortedBy { it.at })

    /** Drops the latest event — the "undo last" button. */
    fun withoutLastEvent(): LifeState =
        if (events.isEmpty()) this else copy(events = events.dropLast(1))

    /** The latest event by time, if the log is not empty. */
    val lastEvent: LifeEvent? get() = events.lastOrNull()

    /** Events on the calendar day [date] in [zone], newest first. */
    fun eventsOn(date: LocalDate, zone: ZoneId): List<LifeEvent> =
        events.filter { it.at.atZone(zone).toLocalDate() == date }.reversed()

    /** Events recorded strictly after [after] — whatever has not reached sync yet. */
    fun eventsAfter(after: Instant?): List<LifeEvent> =
        if (after == null) events else events.filter { it.at.isAfter(after) }

    /**
     * How many times each type has been logged. Used to suggest pinning what you
     * actually use rather than whatever happens to be first in the list.
     */
    fun usageCounts(): Map<EventType, Int> = events.groupingBy { it.type }.eachCount()

    /** Total correction for the calendar day [date] in [zone]. */
    fun deltaOn(date: LocalDate, zone: ZoneId): Int =
        events.filter { it.at.atZone(zone).toLocalDate() == date }.sumOf { it.deltaMinutes }

    /**
     * Summary for the ISO week containing [reference].
     *
     * A week rather than a day because the allowances are weekly too, and because
     * one bad evening says much less than a bad week.
     */
    fun weekSummary(reference: Instant, zone: ZoneId): WeekSummary {
        val inWeek = events.filter { DosingPeriod.WEEK.isSamePeriod(it.at, reference, zone) }
        val byType = inWeek.groupBy { it.type }
            .mapValues { (_, list) -> list.sumOf { it.deltaMinutes } }
        return WeekSummary(
            totalMinutes = inWeek.sumOf { it.deltaMinutes },
            eventCount = inWeek.size,
            best = byType.filterValues { it > 0 }.maxByOrNull { it.value }?.toPair(),
            worst = byType.filterValues { it < 0 }.minByOrNull { it.value }?.toPair(),
        )
    }

    /**
     * Streaks worth showing, longest first.
     *
     * Only types that appear in the log at all are considered: "never smoked" is
     * an infinite streak and says nothing. For costly actions a streak counts
     * days since the last one; for beneficial actions it counts consecutive days
     * that had at least one.
     */
    fun streaks(today: LocalDate, zone: ZoneId): List<Streak> {
        val seen = events.map { it.type }.toSet()
        return seen.mapNotNull { type ->
            val days = events.filter { it.type == type }.map { it.at.atZone(zone).toLocalDate() }
            val length = if (type.isGain) consecutiveDaysEnding(today, days.toSet()) else daysSince(today, days.max())
            if (length >= MIN_STREAK_DAYS) Streak(type, length, clean = !type.isGain) else null
        }.sortedByDescending { it.days }
    }

    private fun daysSince(today: LocalDate, last: LocalDate): Int =
        (today.toEpochDay() - last.toEpochDay()).toInt().coerceAtLeast(0)

    private fun consecutiveDaysEnding(today: LocalDate, days: Set<LocalDate>): Int {
        // A streak may legitimately end yesterday: today might simply not have
        // happened yet. Starting from today would reset every morning.
        var cursor = if (today in days) today else today.minusDays(1)
        var length = 0
        while (cursor in days) {
            length++
            cursor = cursor.minusDays(1)
        }
        return length
    }

    companion object {
        val DEFAULT_BIRTH_DATE: LocalDate = LocalDate.of(1994, 2, 4)
        const val DEFAULT_BASE_EXPECTANCY_YEARS: Double = 80.0

        /** Below this a "streak" is just noise. */
        const val MIN_STREAK_DAYS: Int = 2
    }
}

/** What a week added up to. */
data class WeekSummary(
    val totalMinutes: Int,
    val eventCount: Int,
    /** The type that gave the most, with its total. */
    val best: Pair<EventType, Int>?,
    /** The type that took the most, with its total. */
    val worst: Pair<EventType, Int>?,
) {
    val isEmpty: Boolean get() = eventCount == 0
}

/**
 * A run of days. [clean] tells the two kinds apart: days without a costly action
 * versus days with a beneficial one.
 */
data class Streak(
    val type: EventType,
    val days: Int,
    val clean: Boolean,
)
