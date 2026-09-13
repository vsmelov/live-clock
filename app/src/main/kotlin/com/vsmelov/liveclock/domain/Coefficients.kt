package com.vsmelov.liveclock.domain

/**
 * THE ONLY PLACE WHERE COEFFICIENTS LIVE. Edit here.
 *
 * The unit is minutes of life expectancy. Negative shortens, positive lengthens.
 * One microlife (Spiegelhalter) is 30 minutes, see [Microlives].
 *
 * Values derived from a hazard ratio are computed by the paper's own formula
 * rather than eyeballed: the source HR is visible right next to each. The proof
 * for every action lives in [EventType] and is shown behind the "i" button.
 *
 * IMPORTANT ABOUT UNITS. Almost all of this epidemiology measures "per day of a
 * lifelong habit from age 35". So -30 for a serving of meat means "eating a
 * serving every day for decades", not "had a steak, lost half an hour". A tap
 * coarsens that into a single event, which is convenient but worth remembering.
 *
 * That is also why several actions carry a [Dosing] allowance: the studies
 * measure an EXTRA serving on top of habit, so ordinary consumption should cost
 * nothing and only the excess should be charged.
 *
 * To add a type: a constant here plus an entry in scratchpad/actions.py, which
 * generates [EventType] and both locales' strings.
 */
object Coefficients {

    /** One microlife per Spiegelhalter — half an hour of life expectancy. */
    const val MICROLIFE_MINUTES: Int = Microlives.MINUTES

    // ───────────────────────── Table from BMJ 2012;345:e8223 ─────────────────────────
    // Taken as published. The author rounds to whole microlives, so these are a
    // little coarser than the formula would give: meat is -40 by the formula and
    // -30 in the table. The table's own numbers were kept.

    /** A cigarette: two cigarettes make one microlife. */
    const val SMOKE: Int = -15

    /** Red meat, an 85 g (3 oz) serving — one microlife. HR 1.13. */
    const val RED_MEAT: Int = -30

    /** A burger — one microlife. Named separately from meat in the paper's text. */
    const val FAST_FOOD: Int = -30

    /** Two hours of screen time make one microlife, so half of that per hour. HR 1.08. */
    const val TV: Int = -15

    /** Coffee: two or three cups a day make one microlife, hence a third per cup. HR 0.90. */
    const val COFFEE: Int = 10

    /**
     * Vegetables and fruit, one serving.
     *
     * The table gives +4 microlives for FIVE OR MORE servings a day, not for one.
     * Divided by five here: the division is linear and absent from the source,
     * but there is no other way to price a "had a serving" button.
     */
    const val VEGETABLES: Int = 24

    /** Walking, per kilometre. From the extended table, +0.4 microlives. */
    const val WALK: Int = 12

    /** A day in heavily polluted air: Mexico City against London, -0.6 microlives. */
    const val AIR_POLLUTION: Int = -18

    // ──────────────── Computed by the formula from individual studies ────────────────
    // Microlives.minutesPerDay(HR) is the same formula the table's author uses.
    // The source hazard ratio is visible in the call itself.

    /** Went to bed after 2 am. UK Biobank, 433,268 people, HR 1.10. */
    val LATE_NIGHT: Int = Microlives.minutesPerDay(hazardRatio = 1.10)

    /** Slept under seven hours. Meta-analysis, HR 1.14 against the 7–8 hour reference. */
    val SLEEP_SHORT: Int = Microlives.minutesPerDay(hazardRatio = 1.14)

    /**
     * Time with people, per day.
     *
     * Derived from social isolation (OR 1.29) rather than from the "strong versus
     * weak ties" contrast (OR 1.50): the latter is an extreme one, like comparing
     * a smoker with a never-smoker, and would overstate a single button.
     */
    val SOCIAL: Int = Microlives.minutesPerDay(hazardRatio = 1.0 / 1.29)

    /** Time among greenery. Meta-analysis, HR 0.96 per +0.1 NDVI around the home. */
    val NATURE: Int = Microlives.minutesPerDay(hazardRatio = 0.96)

    /** Sauna. Kuopio, 2,315 men, 20.7 years. 4–7 times a week against one, HR 0.60. */
    val SAUNA: Int = Microlives.minutesPerDay(hazardRatio = 0.60)

    /** Nuts, a handful a day. Harvard cohorts, 7+ times a week, HR 0.80. */
    val NUTS: Int = Microlives.minutesPerDay(hazardRatio = 0.80)

    /** Cycled to work instead of driving. UK Biobank, 263,450 people, HR 0.59. */
    val BIKE_COMMUTE: Int = Microlives.minutesPerDay(hazardRatio = 0.59)

    /** A run, even five minutes. 55,137 people, 15 years, HR 0.70. */
    val RUNNING: Int = Microlives.minutesPerDay(hazardRatio = 0.70)

    // ───────────────────────── Checked, no effect found ─────────────────────────
    // Zero here is a result, not a placeholder. The button stays so that it is
    // visible that somebody looked and found nothing.

    /** Multivitamins. 390,124 people, 20+ years — no mortality benefit. */
    const val MULTIVITAMIN: Int = 0

    /**
     * A glass of water.
     *
     * The famous hydration study measured sodium in blood, not water drunk. That
     * "drinking more water" extends life has not been shown.
     */
    const val WATER: Int = 0

    // ───────────────────────── Estimates, not studies ─────────────────────────
    // The direction of the effect is widely accepted, but no specific study was
    // at hand. Sizes were set to sit sensibly beside neighbouring rows so that
    // the app nudges the right way. Do not look for precision here.

    /** Fish, one serving. */
    const val FISH: Int = 20

    /** A sugary drink. */
    const val SUGARY_DRINK: Int = -20

    /** Sweets: a dessert, a chocolate bar, a biscuit. */
    const val SWEETS: Int = -10

    /** A day of heavy stress or conflict. */
    const val STRESS: Int = -20

    /** Reading: half an hour of a book. */
    const val READING: Int = 15

    /** Dental floss. */
    const val FLOSS: Int = 10

    /** Tea. */
    const val TEA: Int = 5

    /** A short daytime nap. */
    const val NAP: Int = 5

    /** A seasonal flu vaccination. */
    const val FLU_SHOT: Int = 15

    // ───────────────────────── An estimate, not a source ─────────────────────────

    /**
     * Sex.
     *
     * Working the Caerphilly figure through directly gives about +530 minutes per
     * event, which is plainly implausible. A conservative figure at the level of a
     * workout was taken instead. That is my decision, not a number from the study.
     */
    const val SEX: Int = 60

    // ───────────────────────── Weak provenance ─────────────────────────
    // These rows are absent from the BMJ paper — they appear only in The
    // Conversation's extended table, with no study cited.

    /** A cat or a dog: a day with a pet is one microlife. */
    const val PET: Int = 30

    /** A day of mental exertion — four microlives. */
    const val ACTIVE_MIND: Int = 120

    // ───────────────────────── Chosen by the user ─────────────────────────

    /**
     * A glass of alcohol.
     *
     * In the table the first drink of the day is +1 microlife and only each of
     * the next up to six goes negative. The [Dosing] allowance on this type now
     * restores exactly that shape; this constant is the cost beyond the first.
     */
    const val DRINK: Int = -30

    /**
     * A workout of 20 minutes or more.
     *
     * The table makes the first 20 minutes worth two microlives (+60), with the
     * next forty adding half to one more. This is one microlife, per the brief.
     * Set 60 to match the source.
     */
    const val WORKOUT: Int = 30

    /** Deliberate rest or breathing practice. No source has a row for this. */
    const val REST: Int = 15
}
