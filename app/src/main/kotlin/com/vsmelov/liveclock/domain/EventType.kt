package com.vsmelov.liveclock.domain

/**
 * Типы действий, влияющих на остаток жизни.
 *
 * Расширяется добавлением одной строки: величина берётся из [Coefficients],
 * а весь UI (поиск и сетка в Activity, закреплённые кнопки виджета) и синк
 * строятся из [EventType.entries], поэтому больше ничего править не нужно.
 *
 * [id] — стабильный ключ для сериализации. Его нельзя менять после релиза:
 * по нему читается уже сохранённый лог и список закреплённых кнопок.
 * Переименование записи enum'а или смена их порядка безопасны ровно потому,
 * что сериализуется [id], а не `name`.
 *
 * [keywords] — дополнительные слова для поиска. Сюда стоит класть то, как
 * действие называется в голове, а не в интерфейсе: «сижка», «бухло», «зал».
 */
enum class EventType(
    val id: String,
    val deltaMinutes: Int,
    val label: String,
    val emoji: String,
    val keywords: String = "",
) {
    SMOKE(
        id = "smoke",
        deltaMinutes = Coefficients.SMOKE,
        label = "Покурил",
        emoji = "🚬",
        keywords = "сигарета курить сижка табак дым smoke",
    ),
    DRINK(
        id = "drink",
        deltaMinutes = Coefficients.DRINK,
        label = "Выпил",
        emoji = "🍷",
        keywords = "алкоголь бокал пиво вино бухло drink alcohol",
    ),
    REST(
        id = "rest",
        deltaMinutes = Coefficients.REST,
        label = "Отдохнул",
        emoji = "🧘",
        keywords = "отдых дыхание медитация пауза rest breathe",
    ),
    WORKOUT(
        id = "workout",
        deltaMinutes = Coefficients.WORKOUT,
        label = "Тренировка",
        emoji = "🏋",
        keywords = "спорт зал качалка бег workout gym sport",
    ),
    WALK(
        id = "walk",
        deltaMinutes = Coefficients.WALK,
        label = "Километр пешком",
        emoji = "🚶",
        keywords = "ходьба прогулка шаги walk",
    ),
    COFFEE(
        id = "coffee",
        deltaMinutes = Coefficients.COFFEE,
        label = "Кофе",
        emoji = "☕",
        keywords = "кофе эспрессо капучино coffee",
    ),
    VEGETABLES(
        id = "vegetables",
        deltaMinutes = Coefficients.VEGETABLES,
        label = "Овощи, порция",
        emoji = "🥦",
        keywords = "овощи салат фрукты зелень vegetables",
    ),
    RED_MEAT(
        id = "red_meat",
        deltaMinutes = Coefficients.RED_MEAT,
        label = "Красное мясо, 85 г",
        emoji = "🥩",
        keywords = "мясо стейк бекон колбаса meat",
    ),
    TV(
        id = "tv",
        deltaMinutes = Coefficients.TV,
        label = "Час у экрана",
        emoji = "📺",
        keywords = "телевизор сериал ютуб скроллинг tv",
    ),
    PET(
        id = "pet",
        deltaMinutes = Coefficients.PET,
        label = "С питомцем",
        emoji = "🐕",
        keywords = "кот собака питомец pet",
    ),
    ACTIVE_MIND(
        id = "active_mind",
        deltaMinutes = Coefficients.ACTIVE_MIND,
        label = "Учился",
        emoji = "🧠",
        keywords = "мозг учёба чтение книга задача mind study",
    ),
    ;

    /** Событие удлиняет жизнь. */
    val isGain: Boolean get() = deltaMinutes >= 0

    /** Величина события в микрожизнях. */
    val microlives: Double get() = deltaMinutes.toDouble() / Coefficients.MICROLIFE_MINUTES

    /** Подходит ли тип под поисковый запрос. Регистр и раскладка не важны. */
    fun matches(query: String): Boolean {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return true
        return label.lowercase().contains(needle) ||
            id.contains(needle) ||
            keywords.contains(needle)
    }

    companion object {
        /** Разбор сохранённого [id]. `null` — тип удалён из кода, событие пропускается. */
        fun fromId(id: String): EventType? = entries.firstOrNull { it.id == id }

        /** Что закреплено на виджете, пока пользователь не выбрал своё. */
        val DEFAULT_PINNED: List<EventType> = listOf(SMOKE, REST)
    }
}
