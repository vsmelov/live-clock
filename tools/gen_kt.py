# -*- coding: utf-8 -*-
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from actions import ACTIONS

HEADER = '''package com.vsmelov.liveclock.domain

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
'''

FOOTER = '''    ;

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
'''

def entry(a):
    name = a["id"].upper()
    lines = ["    %s(" % name,
             '        id = "%s",' % a["id"],
             "        deltaMinutes = %s," % a["coef"],
             "        labelRes = R.string.action_%s_label," % a["id"],
             '        emoji = "%s",' % a["emoji"],
             "        keywordsRes = R.string.action_%s_keywords," % a["id"],
             "        evidence = Evidence(",
             "            confidence = Confidence.%s," % a["conf"],
             "            exposureRes = R.string.action_%s_exposure," % a["id"],
             "            basisRes = R.string.action_%s_basis," % a["id"],
             "            caveatRes = R.string.action_%s_caveat," % a["id"],
             '            sourceTitle = "%s",' % a["src"][0],
             '            sourceUrl = "%s",' % a["src"][1],
             "        ),"]
    if a.get("dosing"):
        period, normal, within, beyond = a["dosing"]
        lines += ["        dosing = Dosing(",
                  "            period = DosingPeriod.%s," % period,
                  "            normal = %d," % normal,
                  "            withinNormalMinutes = %s," % within,
                  "            beyondNormalMinutes = %s," % beyond,
                  "        ),"]
    lines.append("    ),")
    return "\n".join(lines)

body = "\n".join(entry(a) for a in ACTIONS)
out = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/kotlin/com/vsmelov/liveclock/domain/EventType.kt"
out.write_text(HEADER + body + "\n" + FOOTER, encoding="utf-8")
print("EventType.kt:", len(ACTIONS), "entries")
