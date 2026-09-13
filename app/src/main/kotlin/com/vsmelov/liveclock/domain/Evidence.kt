package com.vsmelov.liveclock.domain

import androidx.annotation.StringRes
import com.vsmelov.liveclock.R

/**
 * Where a coefficient came from. Shown behind the "i" button next to an action.
 *
 * The point is that a number should not be mistakable for truth without seeing
 * what holds it up. Almost this entire field is observational: the association
 * is real, the causation is not established.
 *
 * [sourceTitle] is deliberately a plain string rather than a resource — author
 * names and journal titles are proper nouns and do not get translated.
 */
data class Evidence(
    val confidence: Confidence,
    /** What was actually measured: "one 85 g serving a day, lifelong from 35". */
    @StringRes val exposureRes: Int,
    /** How the number follows from the study. */
    @StringRes val basisRes: Int,
    /** What not to trust here. Never empty — a test enforces it. */
    @StringRes val caveatRes: Int,
    val sourceTitle: String,
    val sourceUrl: String,
)

/** How firmly a number stands. */
enum class Confidence(@StringRes val labelRes: Int) {
    /** Meta-analysis or a very large cohort, effect holds up. */
    STRONG(R.string.confidence_strong),

    /** One large study, or studies that disagree. */
    MODERATE(R.string.confidence_moderate),

    /** Small or old study, reverse causation likely. */
    WEAK(R.string.confidence_weak),

    /** It was checked and no effect on mortality was found. */
    NONE(R.string.confidence_none),

    /**
     * The direction is widely accepted but no specific study was at hand — the
     * size was set to sit sensibly beside neighbouring rows.
     */
    ESTIMATE(R.string.confidence_estimate),

    /** The value was chosen by the user, not by a study. */
    CHOSEN(R.string.confidence_chosen),
}
