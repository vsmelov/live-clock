package com.vsmelov.liveclock.domain

import java.time.Instant

/**
 * Одно записанное действие.
 *
 * [deltaMinutes] копируется из [EventType] в момент записи и дальше живёт
 * своей жизнью. Так правка [Coefficients] не переписывает прошлое: вчерашняя
 * сигарета остаётся стоившей столько, сколько стоила вчера.
 */
data class LifeEvent(
    val type: EventType,
    val at: Instant,
    val deltaMinutes: Int = type.deltaMinutes,
) {
    companion object {
        /** Событие «сейчас» с текущим коэффициентом типа. */
        fun now(type: EventType, at: Instant): LifeEvent =
            LifeEvent(type = type, at = at, deltaMinutes = type.deltaMinutes)
    }
}
