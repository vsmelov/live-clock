package com.vsmelov.liveclock.domain

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Перевод опубликованных hazard ratio в минуты жизни.
 *
 * Формула не выдумана — она из той же статьи, что и таблица
 * (BMJ 2012;345:e8223): «для hazard ratio между 0.75 и 1.3 дневное изменение
 * микрожизней хорошо приближается 10.9·log(r) для мужчин и 9.3·log(r)
 * для женщин».
 *
 * Это даёт способ считать коэффициент для любого исследования, а не только
 * для строк готовой таблицы. Проверка на известных строках:
 *   мясо   HR 1.13 -> -1.33 микрожизни (в таблице -1)
 *   овощи  HR 0.66 -> +4.53            (в таблице +4)
 *   спорт  HR 0.81 -> +2.30            (в таблице +2)
 *   кофе   HR 0.90 -> +1.15            (в таблице +1)
 * Таблица округляет до целых микрожизней, формула — нет.
 */
object Microlives {

    /** Одна микрожизнь — полчаса ожидаемой жизни. */
    const val MINUTES: Int = 30

    /** Коэффициент для мужчин. Для женщин в статье 9.3. */
    private const val MEN_COEFFICIENT = 10.9

    /** Диапазон, в котором авторы называют приближение хорошим. */
    private val TRUSTED_RANGE = 0.75..1.3

    /** Микрожизней в день при пожизненном воздействии с hazard ratio [hazardRatio]. */
    fun perDay(hazardRatio: Double): Double {
        require(hazardRatio > 0) { "hazard ratio должен быть положительным" }
        return -MEN_COEFFICIENT * ln(hazardRatio)
    }

    /** То же в минутах — готовый коэффициент для [Coefficients]. */
    fun minutesPerDay(hazardRatio: Double): Int =
        (perDay(hazardRatio) * MINUTES).roundToInt()

    /** Приближение за пределами этого диапазона авторы не обещают. */
    fun isTrusted(hazardRatio: Double): Boolean = hazardRatio in TRUSTED_RANGE
}
