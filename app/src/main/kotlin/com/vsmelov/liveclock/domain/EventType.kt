package com.vsmelov.liveclock.domain

import androidx.annotation.StringRes
import com.vsmelov.liveclock.R

/**
 * Every action that moves the life estimate.
 *
 * Extending it takes one entry: the value comes from [Coefficients], and search,
 * the action grid, pinning, widget buttons and sync are all built from
 * [EventType.entries], so nothing else needs touching.
 *
 * [id] is the stable storage key. It must not change after the first release:
 * the saved log and the pinned list are read by it. Renaming an enum entry or
 * reordering entries is safe precisely because [id] is what gets serialised.
 *
 * [evidence] is mandatory. A number without a proof does not belong here — the
 * whole point is that you cannot mistake it for truth without seeing what holds
 * it up.
 *
 * Generated from scratchpad/actions.py together with the string resources, so
 * the two locales cannot drift apart.
 */
enum class EventType(
    val id: String,
    val deltaMinutes: Int,
    @StringRes val labelRes: Int,
    val emoji: String,
    /**
     * Search terms, held in a resource that is deliberately NOT localised: it
     * carries both languages at once, so a query in either one finds the action
     * whatever the interface language is set to.
     */
    @StringRes val keywordsRes: Int,
    val evidence: Evidence,
    /**
     * Allowance per period, where an action has one. Inside the allowance an
     * event costs one thing, beyond it another. See [Dosing].
     *
     * [deltaMinutes] stays the headline figure for display; the real price of a
     * particular tap is computed in [LifeState.deltaFor] and frozen into the event.
     */
    val dosing: Dosing? = null,
) {
    SMOKE(
        id = "smoke",
        deltaMinutes = Coefficients.SMOKE,
        labelRes = R.string.action_smoke_label,
        emoji = "🚬",
        keywordsRes = R.string.action_smoke_keywords,
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposureRes = R.string.action_smoke_exposure,
            basisRes = R.string.action_smoke_basis,
            caveatRes = R.string.action_smoke_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
    ),
    DRINK(
        id = "drink",
        deltaMinutes = Coefficients.DRINK,
        labelRes = R.string.action_drink_label,
        emoji = "🍷",
        keywordsRes = R.string.action_drink_keywords,
        evidence = Evidence(
            confidence = Confidence.CHOSEN,
            exposureRes = R.string.action_drink_exposure,
            basisRes = R.string.action_drink_basis,
            caveatRes = R.string.action_drink_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
        dosing = Dosing(
            period = DosingPeriod.DAY,
            normal = 1,
            withinNormalMinutes = 30,
            beyondNormalMinutes = Coefficients.DRINK,
        ),
    ),
    REST(
        id = "rest",
        deltaMinutes = Coefficients.REST,
        labelRes = R.string.action_rest_label,
        emoji = "🧘",
        keywordsRes = R.string.action_rest_keywords,
        evidence = Evidence(
            confidence = Confidence.CHOSEN,
            exposureRes = R.string.action_rest_exposure,
            basisRes = R.string.action_rest_basis,
            caveatRes = R.string.action_rest_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
    ),
    WORKOUT(
        id = "workout",
        deltaMinutes = Coefficients.WORKOUT,
        labelRes = R.string.action_workout_label,
        emoji = "🏋",
        keywordsRes = R.string.action_workout_keywords,
        evidence = Evidence(
            confidence = Confidence.CHOSEN,
            exposureRes = R.string.action_workout_exposure,
            basisRes = R.string.action_workout_basis,
            caveatRes = R.string.action_workout_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
    ),
    SLEEP_SHORT(
        id = "sleep_short",
        deltaMinutes = Coefficients.SLEEP_SHORT,
        labelRes = R.string.action_sleep_short_label,
        emoji = "🥱",
        keywordsRes = R.string.action_sleep_short_keywords,
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposureRes = R.string.action_sleep_short_exposure,
            basisRes = R.string.action_sleep_short_basis,
            caveatRes = R.string.action_sleep_short_caveat,
            sourceTitle = "Sleep and mortality meta-analysis, GeroScience 2025",
            sourceUrl = "https://link.springer.com/article/10.1007/s11357-025-01592-y",
        ),
    ),
    LATE_NIGHT(
        id = "late_night",
        deltaMinutes = Coefficients.LATE_NIGHT,
        labelRes = R.string.action_late_night_label,
        emoji = "🦉",
        keywordsRes = R.string.action_late_night_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_late_night_exposure,
            basisRes = R.string.action_late_night_basis,
            caveatRes = R.string.action_late_night_caveat,
            sourceTitle = "Knutson & von Schantz, Chronobiology International 2018",
            sourceUrl = "https://www.tandfonline.com/doi/full/10.1080/07420528.2018.1454458",
        ),
    ),
    SOCIAL(
        id = "social",
        deltaMinutes = Coefficients.SOCIAL,
        labelRes = R.string.action_social_label,
        emoji = "👥",
        keywordsRes = R.string.action_social_keywords,
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposureRes = R.string.action_social_exposure,
            basisRes = R.string.action_social_basis,
            caveatRes = R.string.action_social_caveat,
            sourceTitle = "Holt-Lunstad et al., Perspectives on Psychological Science 2015",
            sourceUrl = "https://journals.sagepub.com/doi/full/10.1177/1745691614568352",
        ),
    ),
    SEX(
        id = "sex",
        deltaMinutes = Coefficients.SEX,
        labelRes = R.string.action_sex_label,
        emoji = "❤️",
        keywordsRes = R.string.action_sex_keywords,
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposureRes = R.string.action_sex_exposure,
            basisRes = R.string.action_sex_basis,
            caveatRes = R.string.action_sex_caveat,
            sourceTitle = "Davey Smith et al., BMJ 1997 — Sex and death",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/9448525/",
        ),
    ),
    WALK(
        id = "walk",
        deltaMinutes = Coefficients.WALK,
        labelRes = R.string.action_walk_label,
        emoji = "🚶",
        keywordsRes = R.string.action_walk_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_walk_exposure,
            basisRes = R.string.action_walk_basis,
            caveatRes = R.string.action_walk_caveat,
            sourceTitle = "Microlives table, The Conversation",
            sourceUrl = "https://cdn.theconversation.com/assets_for_articles/2017-12-22-microlife-table.pdf",
        ),
    ),
    NATURE(
        id = "nature",
        deltaMinutes = Coefficients.NATURE,
        labelRes = R.string.action_nature_label,
        emoji = "🌲",
        keywordsRes = R.string.action_nature_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_nature_exposure,
            basisRes = R.string.action_nature_basis,
            caveatRes = R.string.action_nature_caveat,
            sourceTitle = "Rojas-Rueda et al., Lancet Planetary Health 2019",
            sourceUrl = "https://www.thelancet.com/journals/lanplh/article/PIIS2542-5196(19)30215-3/fulltext",
        ),
    ),
    AIR_POLLUTION(
        id = "air_pollution",
        deltaMinutes = Coefficients.AIR_POLLUTION,
        labelRes = R.string.action_air_pollution_label,
        emoji = "🏭",
        keywordsRes = R.string.action_air_pollution_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_air_pollution_exposure,
            basisRes = R.string.action_air_pollution_basis,
            caveatRes = R.string.action_air_pollution_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
    ),
    COFFEE(
        id = "coffee",
        deltaMinutes = Coefficients.COFFEE,
        labelRes = R.string.action_coffee_label,
        emoji = "☕",
        keywordsRes = R.string.action_coffee_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_coffee_exposure,
            basisRes = R.string.action_coffee_basis,
            caveatRes = R.string.action_coffee_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
        dosing = Dosing(
            period = DosingPeriod.DAY,
            normal = 3,
            withinNormalMinutes = Coefficients.COFFEE,
            beyondNormalMinutes = 0,
        ),
    ),
    VEGETABLES(
        id = "vegetables",
        deltaMinutes = Coefficients.VEGETABLES,
        labelRes = R.string.action_vegetables_label,
        emoji = "🥦",
        keywordsRes = R.string.action_vegetables_keywords,
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposureRes = R.string.action_vegetables_exposure,
            basisRes = R.string.action_vegetables_basis,
            caveatRes = R.string.action_vegetables_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
        dosing = Dosing(
            period = DosingPeriod.DAY,
            normal = 5,
            withinNormalMinutes = Coefficients.VEGETABLES,
            beyondNormalMinutes = 0,
        ),
    ),
    RED_MEAT(
        id = "red_meat",
        deltaMinutes = Coefficients.RED_MEAT,
        labelRes = R.string.action_red_meat_label,
        emoji = "🥩",
        keywordsRes = R.string.action_red_meat_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_red_meat_exposure,
            basisRes = R.string.action_red_meat_basis,
            caveatRes = R.string.action_red_meat_caveat,
            sourceTitle = "Pan et al., Arch Intern Med 2012",
            sourceUrl = "https://pmc.ncbi.nlm.nih.gov/articles/PMC3712342",
        ),
        dosing = Dosing(
            period = DosingPeriod.WEEK,
            normal = 3,
            withinNormalMinutes = 0,
            beyondNormalMinutes = Coefficients.RED_MEAT,
        ),
    ),
    FAST_FOOD(
        id = "fast_food",
        deltaMinutes = Coefficients.FAST_FOOD,
        labelRes = R.string.action_fast_food_label,
        emoji = "🍔",
        keywordsRes = R.string.action_fast_food_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_fast_food_exposure,
            basisRes = R.string.action_fast_food_basis,
            caveatRes = R.string.action_fast_food_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
        dosing = Dosing(
            period = DosingPeriod.WEEK,
            normal = 1,
            withinNormalMinutes = 0,
            beyondNormalMinutes = Coefficients.FAST_FOOD,
        ),
    ),
    TV(
        id = "tv",
        deltaMinutes = Coefficients.TV,
        labelRes = R.string.action_tv_label,
        emoji = "📺",
        keywordsRes = R.string.action_tv_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_tv_exposure,
            basisRes = R.string.action_tv_basis,
            caveatRes = R.string.action_tv_caveat,
            sourceTitle = "Spiegelhalter, BMJ 2012;345:e8223",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/23247978/",
        ),
        dosing = Dosing(
            period = DosingPeriod.DAY,
            normal = 2,
            withinNormalMinutes = 0,
            beyondNormalMinutes = Coefficients.TV,
        ),
    ),
    PET(
        id = "pet",
        deltaMinutes = Coefficients.PET,
        labelRes = R.string.action_pet_label,
        emoji = "🐕",
        keywordsRes = R.string.action_pet_keywords,
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposureRes = R.string.action_pet_exposure,
            basisRes = R.string.action_pet_basis,
            caveatRes = R.string.action_pet_caveat,
            sourceTitle = "Microlives table, The Conversation",
            sourceUrl = "https://cdn.theconversation.com/assets_for_articles/2017-12-22-microlife-table.pdf",
        ),
    ),
    ACTIVE_MIND(
        id = "active_mind",
        deltaMinutes = Coefficients.ACTIVE_MIND,
        labelRes = R.string.action_active_mind_label,
        emoji = "🧠",
        keywordsRes = R.string.action_active_mind_keywords,
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposureRes = R.string.action_active_mind_exposure,
            basisRes = R.string.action_active_mind_basis,
            caveatRes = R.string.action_active_mind_caveat,
            sourceTitle = "Microlives table, The Conversation",
            sourceUrl = "https://cdn.theconversation.com/assets_for_articles/2017-12-22-microlife-table.pdf",
        ),
    ),
    WATER(
        id = "water",
        deltaMinutes = Coefficients.WATER,
        labelRes = R.string.action_water_label,
        emoji = "💧",
        keywordsRes = R.string.action_water_keywords,
        evidence = Evidence(
            confidence = Confidence.NONE,
            exposureRes = R.string.action_water_exposure,
            basisRes = R.string.action_water_basis,
            caveatRes = R.string.action_water_caveat,
            sourceTitle = "Dmitrieva et al., eBioMedicine 2023",
            sourceUrl = "https://www.thelancet.com/journals/ebiom/article/PIIS2352-3964(22)00586-2/fulltext",
        ),
    ),
    MULTIVITAMIN(
        id = "multivitamin",
        deltaMinutes = Coefficients.MULTIVITAMIN,
        labelRes = R.string.action_multivitamin_label,
        emoji = "💊",
        keywordsRes = R.string.action_multivitamin_keywords,
        evidence = Evidence(
            confidence = Confidence.NONE,
            exposureRes = R.string.action_multivitamin_exposure,
            basisRes = R.string.action_multivitamin_basis,
            caveatRes = R.string.action_multivitamin_caveat,
            sourceTitle = "Loftfield et al., JAMA Network Open 2024",
            sourceUrl = "https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2820369",
        ),
    ),
    SAUNA(
        id = "sauna",
        deltaMinutes = Coefficients.SAUNA,
        labelRes = R.string.action_sauna_label,
        emoji = "🧖",
        keywordsRes = R.string.action_sauna_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_sauna_exposure,
            basisRes = R.string.action_sauna_basis,
            caveatRes = R.string.action_sauna_caveat,
            sourceTitle = "Laukkanen et al., JAMA Internal Medicine 2015",
            sourceUrl = "https://jamanetwork.com/journals/jamainternalmedicine/article-abstract/2448449",
        ),
    ),
    NUTS(
        id = "nuts",
        deltaMinutes = Coefficients.NUTS,
        labelRes = R.string.action_nuts_label,
        emoji = "🥜",
        keywordsRes = R.string.action_nuts_keywords,
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposureRes = R.string.action_nuts_exposure,
            basisRes = R.string.action_nuts_basis,
            caveatRes = R.string.action_nuts_caveat,
            sourceTitle = "Bao et al., NEJM 2013",
            sourceUrl = "https://www.nejm.org/doi/full/10.1056/NEJMoa1307352",
        ),
        dosing = Dosing(
            period = DosingPeriod.DAY,
            normal = 1,
            withinNormalMinutes = Coefficients.NUTS,
            beyondNormalMinutes = 0,
        ),
    ),
    BIKE_COMMUTE(
        id = "bike_commute",
        deltaMinutes = Coefficients.BIKE_COMMUTE,
        labelRes = R.string.action_bike_commute_label,
        emoji = "🚴",
        keywordsRes = R.string.action_bike_commute_keywords,
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposureRes = R.string.action_bike_commute_exposure,
            basisRes = R.string.action_bike_commute_basis,
            caveatRes = R.string.action_bike_commute_caveat,
            sourceTitle = "Celis-Morales et al., BMJ 2017",
            sourceUrl = "https://www.ccam-tac.org/wp-content/uploads/2020/03/bmj.j1456.full_.pdf",
        ),
    ),
    RUNNING(
        id = "running",
        deltaMinutes = Coefficients.RUNNING,
        labelRes = R.string.action_running_label,
        emoji = "🏃",
        keywordsRes = R.string.action_running_keywords,
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposureRes = R.string.action_running_exposure,
            basisRes = R.string.action_running_basis,
            caveatRes = R.string.action_running_caveat,
            sourceTitle = "Lee et al., JACC 2014",
            sourceUrl = "https://www.jacc.org/doi/10.1016/j.jacc.2014.04.058",
        ),
    ),
    FISH(
        id = "fish",
        deltaMinutes = Coefficients.FISH,
        labelRes = R.string.action_fish_label,
        emoji = "🐟",
        keywordsRes = R.string.action_fish_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_fish_exposure,
            basisRes = R.string.action_fish_basis,
            caveatRes = R.string.action_fish_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    SUGARY_DRINK(
        id = "sugary_drink",
        deltaMinutes = Coefficients.SUGARY_DRINK,
        labelRes = R.string.action_sugary_drink_label,
        emoji = "🥤",
        keywordsRes = R.string.action_sugary_drink_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_sugary_drink_exposure,
            basisRes = R.string.action_sugary_drink_basis,
            caveatRes = R.string.action_sugary_drink_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
        dosing = Dosing(
            period = DosingPeriod.WEEK,
            normal = 2,
            withinNormalMinutes = 0,
            beyondNormalMinutes = Coefficients.SUGARY_DRINK,
        ),
    ),
    SWEETS(
        id = "sweets",
        deltaMinutes = Coefficients.SWEETS,
        labelRes = R.string.action_sweets_label,
        emoji = "🍬",
        keywordsRes = R.string.action_sweets_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_sweets_exposure,
            basisRes = R.string.action_sweets_basis,
            caveatRes = R.string.action_sweets_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
        dosing = Dosing(
            period = DosingPeriod.DAY,
            normal = 1,
            withinNormalMinutes = 0,
            beyondNormalMinutes = Coefficients.SWEETS,
        ),
    ),
    STRESS(
        id = "stress",
        deltaMinutes = Coefficients.STRESS,
        labelRes = R.string.action_stress_label,
        emoji = "😤",
        keywordsRes = R.string.action_stress_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_stress_exposure,
            basisRes = R.string.action_stress_basis,
            caveatRes = R.string.action_stress_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    READING(
        id = "reading",
        deltaMinutes = Coefficients.READING,
        labelRes = R.string.action_reading_label,
        emoji = "📖",
        keywordsRes = R.string.action_reading_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_reading_exposure,
            basisRes = R.string.action_reading_basis,
            caveatRes = R.string.action_reading_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    FLOSS(
        id = "floss",
        deltaMinutes = Coefficients.FLOSS,
        labelRes = R.string.action_floss_label,
        emoji = "🦷",
        keywordsRes = R.string.action_floss_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_floss_exposure,
            basisRes = R.string.action_floss_basis,
            caveatRes = R.string.action_floss_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    TEA(
        id = "tea",
        deltaMinutes = Coefficients.TEA,
        labelRes = R.string.action_tea_label,
        emoji = "🫖",
        keywordsRes = R.string.action_tea_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_tea_exposure,
            basisRes = R.string.action_tea_basis,
            caveatRes = R.string.action_tea_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    NAP(
        id = "nap",
        deltaMinutes = Coefficients.NAP,
        labelRes = R.string.action_nap_label,
        emoji = "😴",
        keywordsRes = R.string.action_nap_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_nap_exposure,
            basisRes = R.string.action_nap_basis,
            caveatRes = R.string.action_nap_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    FLU_SHOT(
        id = "flu_shot",
        deltaMinutes = Coefficients.FLU_SHOT,
        labelRes = R.string.action_flu_shot_label,
        emoji = "💉",
        keywordsRes = R.string.action_flu_shot_keywords,
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposureRes = R.string.action_flu_shot_exposure,
            basisRes = R.string.action_flu_shot_basis,
            caveatRes = R.string.action_flu_shot_caveat,
            sourceTitle = "General reference. The number is mine, not from it",
            sourceUrl = "https://www.health.harvard.edu/staying-healthy",
        ),
    ),
    ;

    /** The action lengthens life. */
    val isGain: Boolean get() = deltaMinutes >= 0

    /** The size of the event in microlives. */
    val microlives: Double get() = deltaMinutes.toDouble() / Coefficients.MICROLIFE_MINUTES

    /** The action was checked and no effect on mortality was found. */
    val isNeutral: Boolean get() = deltaMinutes == 0

    companion object {
        /** Parses a stored [id]. `null` means the type was removed from the code. */
        fun fromId(id: String): EventType? = entries.firstOrNull { it.id == id }

        /** What the widget shows until the user pins their own. */
        val DEFAULT_PINNED: List<EventType> = listOf(SMOKE, REST)
    }
}
