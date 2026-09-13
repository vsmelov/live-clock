package com.vsmelov.liveclock.domain

/**
 * Типы действий, влияющих на остаток жизни.
 *
 * Расширяется добавлением одной строки: величина берётся из [Coefficients],
 * а весь UI (сетка в Activity, кнопки виджета) и синк строятся из
 * [EventType.entries], поэтому больше ничего править не нужно.
 *
 * [id] — стабильный ключ для сериализации. Его нельзя менять после релиза:
 * по нему читается уже сохранённый лог. Переименование записи enum'а или
 * смена их порядка безопасны ровно потому, что сериализуется [id], а не `name`.
 */
enum class EventType(
    val id: String,
    val deltaMinutes: Int,
    val label: String,
    val emoji: String,
) {
    SMOKE(id = "smoke", deltaMinutes = Coefficients.SMOKE, label = "Покурил", emoji = "🚬"),
    DRINK(id = "drink", deltaMinutes = Coefficients.DRINK, label = "Выпил", emoji = "🍷"),
    REST(id = "rest", deltaMinutes = Coefficients.REST, label = "Отдохнул", emoji = "🧘"),
    WORKOUT(id = "workout", deltaMinutes = Coefficients.WORKOUT, label = "Тренировка", emoji = "🏋"),
    ;

    /** Событие удлиняет жизнь. */
    val isGain: Boolean get() = deltaMinutes >= 0

    /** Величина события в микрожизнях. */
    val microlives: Double get() = deltaMinutes.toDouble() / Coefficients.MICROLIFE_MINUTES

    companion object {
        /** Разбор сохранённого [id]. `null` — тип удалён из кода, событие пропускается. */
        fun fromId(id: String): EventType? = entries.firstOrNull { it.id == id }
    }
}
