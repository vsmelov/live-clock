package com.vsmelov.liveclock.domain

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Converts published hazard ratios into minutes of life.
 *
 * The formula is not invented — it comes from the same paper as the table
 * (BMJ 2012;345:e8223): "for hazard ratios between 0.75 and 1.3, the daily
 * change in microlives is well approximated by 10.9·log(r) for men and
 * 9.3·log(r) for women".
 *
 * That gives a way to derive a coefficient from any study rather than only from
 * the rows of a ready-made table. Checked against the published rows:
 *   red meat   HR 1.13 -> -1.33 microlives (table says -1)
 *   vegetables HR 0.66 -> +4.53            (table says +4)
 *   exercise   HR 0.81 -> +2.30            (table says +2)
 *   coffee     HR 0.90 -> +1.15            (table says +1)
 * The table rounds to whole microlives, the formula does not.
 */
object Microlives {

    /** One microlife is half an hour of life expectancy. */
    const val MINUTES: Int = 30

    /** The men's coefficient. The paper gives 9.3 for women. */
    private const val MEN_COEFFICIENT = 10.9

    /** The range in which the authors call the approximation good. */
    private val TRUSTED_RANGE = 0.75..1.3

    /** Microlives per day under lifelong exposure with the given [hazardRatio]. */
    fun perDay(hazardRatio: Double): Double {
        require(hazardRatio > 0) { "a hazard ratio must be positive" }
        return -MEN_COEFFICIENT * ln(hazardRatio)
    }

    /** The same in minutes — a ready coefficient for [Coefficients]. */
    fun minutesPerDay(hazardRatio: Double): Int =
        (perDay(hazardRatio) * MINUTES).roundToInt()

    /** Outside this range the authors promise nothing about the approximation. */
    fun isTrusted(hazardRatio: Double): Boolean = hazardRatio in TRUSTED_RANGE
}
