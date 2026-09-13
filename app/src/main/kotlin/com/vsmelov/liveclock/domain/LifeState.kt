package com.vsmelov.liveclock.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Полное состояние приложения. Источник правды — локальный DataStore,
 * который хранит ровно это.
 *
 * [events] всегда отсортирован по [LifeEvent.at] по возрастанию — это
 * поддерживают [plusEvent] и [withoutLastEvent].
 */
data class LifeState(
    val birthDate: LocalDate = DEFAULT_BIRTH_DATE,
    val baseExpectancyYears: Double = DEFAULT_BASE_EXPECTANCY_YEARS,
    val events: List<LifeEvent> = emptyList(),
) {

    /** Суммарная поправка в минутах по всему логу. */
    val totalDeltaMinutes: Int get() = events.sumOf { it.deltaMinutes }

    /**
     * Сколько будет стоить событие [type], записанное в момент [at].
     *
     * У типов с [EventType.dosing] величина зависит от того, сколько таких
     * событий уже есть в периоде: в пределах нормы одна цена, сверх — другая.
     * Считается в момент записи и замораживается в [LifeEvent.deltaMinutes],
     * поэтому лог остаётся честной записью того, что происходило.
     */
    fun deltaFor(type: EventType, at: Instant, zone: ZoneId): Int {
        val dosing = type.dosing ?: return type.deltaMinutes
        return if (countInPeriod(type, at, zone) < dosing.normal) {
            dosing.withinNormalMinutes
        } else {
            dosing.beyondNormalMinutes
        }
    }

    /** Сколько событий типа [type] уже записано в периоде, куда попадает [at]. */
    fun countInPeriod(type: EventType, at: Instant, zone: ZoneId): Int {
        val dosing = type.dosing ?: return 0
        return events.count { it.type == type && dosing.period.isSamePeriod(it.at, at, zone) }
    }

    /** Сколько ещё осталось до перебора. Отрицательных не бывает. */
    fun remainingInNorm(type: EventType, at: Instant, zone: ZoneId): Int {
        val dosing = type.dosing ?: return 0
        return (dosing.normal - countInPeriod(type, at, zone)).coerceAtLeast(0)
    }

    /** Добавляет событие, сохраняя сортировку по времени. */
    fun plusEvent(event: LifeEvent): LifeState =
        copy(events = (events + event).sortedBy { it.at })

    /** Убирает последнее по времени событие — кнопка «отменить последнее». */
    fun withoutLastEvent(): LifeState =
        if (events.isEmpty()) this else copy(events = events.dropLast(1))

    /** Последнее по времени событие, если лог не пуст. */
    val lastEvent: LifeEvent? get() = events.lastOrNull()

    /** События за календарные сутки [date] в зоне [zone], новые сверху. */
    fun eventsOn(date: LocalDate, zone: ZoneId): List<LifeEvent> =
        events.filter { it.at.atZone(zone).toLocalDate() == date }.reversed()

    /**
     * Сколько раз каждый тип попадал в лог. Нужен, чтобы предлагать
     * закрепить то, чем реально пользуешься, а не то, что стоит первым.
     */
    fun usageCounts(): Map<EventType, Int> =
        events.groupingBy { it.type }.eachCount()

    /** Суммарная поправка за календарные сутки [date] в зоне [zone]. */
    fun deltaOn(date: LocalDate, zone: ZoneId): Int =
        events.filter { it.at.atZone(zone).toLocalDate() == date }.sumOf { it.deltaMinutes }

    /** События, записанные строго после [after] — то, что ещё не ушло в синк. */
    fun eventsAfter(after: Instant?): List<LifeEvent> =
        if (after == null) events else events.filter { it.at.isAfter(after) }

    companion object {
        val DEFAULT_BIRTH_DATE: LocalDate = LocalDate.of(1994, 2, 4)
        const val DEFAULT_BASE_EXPECTANCY_YEARS: Double = 80.0
    }
}
