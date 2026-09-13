package com.vsmelov.liveclock.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Весь расчёт остатка жизни. Чистые функции без состояния и без android.*,
 * поэтому проверяются обычными JVM-тестами.
 *
 * Формула: ожидаемый момент смерти — это дата рождения плюс базовая ожидаемая
 * продолжительность плюс сумма всех дельт лога. Остаток — разница между этим
 * моментом и «сейчас».
 */
object LifeMath {

    /**
     * Средняя длина года григорианского календаря в сутках.
     *
     * Целые годы прибавляются календарно (через [java.time.LocalDate.plusYears]),
     * поэтому високосные годы учитываются точно. Эта константа нужна только
     * для дробной части ожидаемой продолжительности: 80.5 года — это 80 полных
     * календарных лет плюс полгода, посчитанных по средней длине года.
     */
    const val DAYS_PER_YEAR: Double = 365.2425

    /** Секунд в среднем году — база для пересчёта остатка в годы. */
    const val SECONDS_PER_YEAR: Double = DAYS_PER_YEAR * 24.0 * 60.0 * 60.0

    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val NANOS_PER_SECOND = 1_000_000_000.0

    /**
     * Ожидаемый момент смерти с учётом всего лога.
     *
     * Отсчёт ведётся от полуночи дня рождения в зоне [zone]. Отрицательная
     * базовая продолжительность бессмысленна и поджимается к нулю.
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
     * Остаток жизни на момент [now].
     *
     * Значение знаковое: если ожидаемый момент уже позади, вернётся
     * отрицательная длительность. Это осознанно — врать про нули незачем.
     */
    fun remaining(state: LifeState, now: Instant, zone: ZoneId): Duration =
        Duration.between(now, expectedDeathInstant(state, zone))

    /** Тот же остаток, выраженный в годах. Знаковый, см. [remaining]. */
    fun remainingYears(state: LifeState, now: Instant, zone: ZoneId): Double {
        val left = remaining(state, now, zone)
        return (left.seconds + left.nano / NANOS_PER_SECOND) / SECONDS_PER_YEAR
    }

    /**
     * Остаток в годах с четырьмя знаками после запятой, как на виджете.
     * Разделитель — точка независимо от локали устройства.
     */
    fun formatYears(years: Double): String = "%.4f".format(Locale.US, years)

    /** Готовая строка для крупной надписи виджета. */
    fun formatRemainingYears(state: LifeState, now: Instant, zone: ZoneId): String =
        formatYears(remainingYears(state, now, zone))

    /**
     * Доля прожитого: 0.0 в день рождения, 1.0 в ожидаемый момент.
     *
     * Считается от полуночи дня рождения до ожидаемого момента, то есть
     * с учётом всего лога: каждая сигарета двигает не только остаток,
     * но и знаменатель.
     */
    fun elapsedFraction(state: LifeState, now: Instant, zone: ZoneId): Double {
        val birth = state.birthDate.atStartOfDay(zone).toInstant()
        val death = expectedDeathInstant(state, zone)
        val total = Duration.between(birth, death).seconds
        if (total <= 0) return 1.0
        val lived = Duration.between(birth, now).seconds
        return (lived.toDouble() / total).coerceIn(0.0, 1.0)
    }

    /** Доля прожитого в процентах с тремя знаками: «63.874%». */
    fun formatElapsedPercent(fraction: Double): String =
        "%.3f%%".format(Locale.US, fraction * 100.0)

    /**
     * Остаток одной строкой, как на виджете: «17310д 1:56:26».
     *
     * Сутки подставляются в формат Chronometer'а, а «Ч:ММ:СС» он дорисовывает
     * сам и сам же тикает. Символ процента в подставляемой части экранируется:
     * формат уходит в String.format внутри Chronometer.
     */
    fun chronometerFormat(days: Long): String = "${days}д %s"

    /**
     * Остаток одной строкой с секундами: «17310д 23:21:45».
     *
     * Для Activity, где секунды можно честно перерисовывать каждую секунду.
     * На виджете тот же вид собирается из [chronometerFormat] и Chronometer'а,
     * потому что там раз в секунду нас никто будить не станет.
     */
    fun formatCountdown(state: LifeState, now: Instant, zone: ZoneId): String {
        val days = remainingWholeDays(state, now, zone)
        val within = remainingWithinDay(state, now, zone)
        return "%dд %d:%02d:%02d".format(
            Locale.US,
            days,
            within.toHours(),
            within.toMinutes() % MINUTES_PER_HOUR,
            within.seconds % SECONDS_PER_MINUTE,
        )
    }

    /**
     * Целых суток в остатке.
     *
     * Виджет показывает сутки отдельным числом, а Chronometer тикает только
     * остатком внутри суток — см. [remainingWithinDay]. Chronometer умеет
     * форматировать лишь «Ч:ММ:СС», поэтому 17310 суток он показал бы как
     * 415440 часов.
     */
    fun remainingWholeDays(state: LifeState, now: Instant, zone: ZoneId): Long {
        val left = remaining(state, now, zone)
        return if (left.isNegative) 0 else left.toDays()
    }

    /**
     * Остаток за вычетом целых суток — то, что тикает секундами на виджете.
     * Всегда в диапазоне от нуля до суток.
     */
    fun remainingWithinDay(state: LifeState, now: Instant, zone: ZoneId): Duration {
        val left = remaining(state, now, zone)
        if (left.isNegative) return Duration.ZERO
        return left.minusDays(left.toDays())
    }

    /**
     * Начало следующих календарных суток в зоне [zone] — база обратного
     * отсчёта Chronometer'а на виджете.
     *
     * [java.time.LocalDate.atStartOfDay] корректно разбирается с переводом
     * часов: если полуночи в этот день не существует, вернётся первый
     * существующий момент суток.
     */
    fun startOfNextDay(now: Instant, zone: ZoneId): Instant =
        now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
}
