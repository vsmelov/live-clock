package com.vsmelov.liveclock.domain

import java.time.Instant

/**
 * One recorded action.
 *
 * [deltaMinutes] is computed when the event is written and then lives its own
 * life. That way editing [Coefficients] does not rewrite the past: yesterday's
 * cigarette stays worth what it was worth yesterday. The same applies to an
 * allowance — an event charged as excess stays charged even if earlier events
 * are later undone.
 */
data class LifeEvent(
    val type: EventType,
    val at: Instant,
    val deltaMinutes: Int = type.deltaMinutes,
)
