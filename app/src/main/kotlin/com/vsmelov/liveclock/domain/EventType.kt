package com.vsmelov.liveclock.domain

private const val BMJ = "https://pubmed.ncbi.nlm.nih.gov/23247978/"
private const val BMJ_TITLE = "Spiegelhalter, BMJ 2012;345:e8223 — микрожизни"
private const val CONVERSATION =
    "https://cdn.theconversation.com/assets_for_articles/2017-12-22-microlife-table.pdf"

/**
 * Типы действий, влияющих на остаток жизни.
 *
 * Расширяется одной строкой: величина берётся из [Coefficients], а поиск,
 * сетка, закрепление, кнопки виджета и синк строятся из [EventType.entries].
 *
 * [id] — стабильный ключ для хранения. После первого запуска его менять
 * нельзя: по нему читается сохранённый лог и список закреплённых. Зато
 * переименование записи enum'а и перестановка безопасны.
 *
 * [evidence] обязательно. Цифру без пруфа сюда класть нельзя — весь смысл
 * в том, чтобы её нельзя было принять за истину, не увидев, на чём она стоит.
 */
enum class EventType(
    val id: String,
    val deltaMinutes: Int,
    val label: String,
    val emoji: String,
    val keywords: String,
    val evidence: Evidence,
) {
    SMOKE(
        id = "smoke", deltaMinutes = Coefficients.SMOKE,
        label = "Покурил", emoji = "🚬",
        keywords = "сигарета курить сижка табак дым smoke",
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposure = "Две сигареты в день = одна микрожизнь",
            basis = "Таблица микрожизней; связь курения со смертностью — одна " +
                "из самых твёрдых в эпидемиологии вообще",
            caveat = "Величина усреднена по популяции и по всей жизни куряшего. " +
                "Одна конкретная сигарета ничего не «списывает» буквально",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    DRINK(
        id = "drink", deltaMinutes = Coefficients.DRINK,
        label = "Выпил", emoji = "🍷",
        keywords = "алкоголь бокал пиво вино бухло drink alcohol",
        evidence = Evidence(
            confidence = Confidence.CHOSEN,
            exposure = "Одна доза (10 г спирта)",
            basis = "Значение из твоего ТЗ. В источнике алкоголь нелинеен: первая " +
                "доза за день +1 микрожизнь, каждая следующая до шести — в минус",
            caveat = "Плоский минус проще жать, но источнику противоречит. " +
                "Поставь +30 в Coefficients, если хочешь как в статье",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    REST(
        id = "rest", deltaMinutes = Coefficients.REST,
        label = "Отдохнул", emoji = "🧘",
        keywords = "отдых дыхание медитация пауза rest breathe",
        evidence = Evidence(
            confidence = Confidence.CHOSEN,
            exposure = "Осознанная пауза или дыхательная практика",
            basis = "Значение из твоего ТЗ",
            caveat = "Строки про осознанный отдых нет ни в одном из источников. " +
                "Это мотивационная кнопка, а не измеренный эффект",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    WORKOUT(
        id = "workout", deltaMinutes = Coefficients.WORKOUT,
        label = "Тренировка", emoji = "🏋",
        keywords = "спорт зал качалка бег workout gym sport",
        evidence = Evidence(
            confidence = Confidence.CHOSEN,
            exposure = "Первые 20 минут умеренной нагрузки, HR 0.81",
            basis = "Значение из твоего ТЗ. Источник даёт вдвое больше: " +
                "первые 20 минут = 2 микрожизни (+60), следующие сорок — ещё до одной",
            caveat = "Здесь ты недооцениваешь тренировку вдвое. Поставь 60, " +
                "чтобы совпало с источником",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    SLEEP_SHORT(
        id = "sleep_short", deltaMinutes = Coefficients.SLEEP_SHORT,
        label = "Недосып, меньше 7 ч", emoji = "🥱",
        keywords = "сон недосып мало спал не выспался sleep",
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposure = "Меньше 7 часов сна за ночь против нормы 7–8",
            basis = "Мета-анализ, HR 1.14 → −1.43 микрожизни в день по формуле статьи. " +
                "Связь U-образная: 9+ часов ещё хуже, HR 1.34",
            caveat = "Кнопки «выспался» нет намеренно: 7–8 часов — это точка отсчёта, " +
                "а не бонус. Длинный сон почти наверняка обратная причинность — " +
                "больные спят дольше",
            sourceTitle = "Мета-анализ сна и смертности, GeroScience 2025",
            sourceUrl = "https://link.springer.com/article/10.1007/s11357-025-01592-y",
        ),
    ),
    LATE_NIGHT(
        id = "late_night", deltaMinutes = Coefficients.LATE_NIGHT,
        label = "Лёг после 2 ночи", emoji = "🦉",
        keywords = "сова поздно ночь режим сдвиг хронотип owl",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Выраженный вечерний хронотип против утреннего",
            basis = "UK Biobank, 433 268 человек, 6.5 лет. HR 1.10 → −1.04 микрожизни " +
                "в день по формуле статьи",
            caveat = "Мерили самоопределение «сова или жаворонок», а не время отбоя. " +
                "И это скорее про рассогласование с графиком работы, чем про сам " +
                "хронотип: более поздняя работа часть вреда снимает",
            sourceTitle = "Knutson & von Schantz, Chronobiology International 2018",
            sourceUrl = "https://www.tandfonline.com/doi/full/10.1080/07420528.2018.1454458",
        ),
    ),
    SOCIAL(
        id = "social", deltaMinutes = Coefficients.SOCIAL,
        label = "Живое общение", emoji = "👥",
        keywords = "друзья общение встреча люди социальное social",
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposure = "Отсутствие социальной изоляции, за день",
            basis = "Мета-анализ: 148 работ, 308 849 человек. Взят OR 1.29 для изоляции " +
                "→ +2.78 микрожизни в день. Контраст «сильные против слабых связей» " +
                "даёт 1.50, но он крайний и для одной кнопки завышал бы",
            caveat = "Самый большой плюс в списке — и это не ошибка: по силе связи " +
                "одиночество сопоставимо с курением. Но одно нажатие не переводит " +
                "человека из изолированных в общительные",
            sourceTitle = "Holt-Lunstad et al., Perspectives on Psychological Science 2015",
            sourceUrl = "https://journals.sagepub.com/doi/full/10.1177/1745691614568352",
        ),
    ),
    SEX(
        id = "sex", deltaMinutes = Coefficients.SEX,
        label = "Секс", emoji = "❤️",
        keywords = "секс интим близость sex",
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposure = "+100 оргазмов в год, мужчины 45–59 лет",
            basis = "Caerphilly, 918 мужчин, 10 лет. OR 0.64 → по формуле около " +
                "+530 минут за раз. Это неправдоподобно много, поэтому взята " +
                "консервативная оценка на уровне тренировки",
            caveat = "Число выбрано мной, а не работой. Выборка мелкая и старая (1997), " +
                "самоотчёт, и почти наверняка обратная причинность: у больных людей " +
                "секса меньше, а не наоборот",
            sourceTitle = "Davey Smith et al., BMJ 1997 — Sex and death",
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/9448525/",
        ),
    ),
    WALK(
        id = "walk", deltaMinutes = Coefficients.WALK,
        label = "Километр пешком", emoji = "🚶",
        keywords = "ходьба прогулка шаги walk",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Километр ходьбы",
            basis = "Расширенная таблица микрожизней, +0.4 микрожизни на километр",
            caveat = "В самой статье BMJ этой строки нет, только в пересказе. " +
                "С основной строкой про нагрузку согласуется по порядку величины",
            sourceTitle = "Таблица микрожизней, The Conversation",
            sourceUrl = CONVERSATION,
        ),
    ),
    NATURE(
        id = "nature", deltaMinutes = Coefficients.NATURE,
        label = "День на природе", emoji = "🌲",
        keywords = "природа лес парк зелень дача nature green",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "+0.1 NDVI зелени в радиусе 500 м от дома",
            basis = "Мета-анализ когорт, HR 0.96 → +0.45 микрожизни в день",
            caveat = "Мерили зелень вокруг жилья, а не вылазки на выходных. " +
                "Разовый выезд этому не равен, так что величина скорее символическая",
            sourceTitle = "Rojas-Rueda et al., Lancet Planetary Health 2019",
            sourceUrl =
                "https://www.thelancet.com/journals/lanplh/article/PIIS2542-5196(19)30215-3/fulltext",
        ),
    ),
    AIR_POLLUTION(
        id = "air_pollution", deltaMinutes = Coefficients.AIR_POLLUTION,
        label = "День в смоге", emoji = "🏭",
        keywords = "смог воздух грязь пыль выхлопы pm25 air",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Жизнь в Мехико против Лондона, −0.6 микрожизни в день",
            basis = "Строка из таблицы микрожизней",
            caveat = "Важно: сам по себе большой город в развитых странах в плюсе, " +
                "а не в минусе — горожане живут дольше сельских, и разрыв растёт. " +
                "Воздух хуже, но доступ к медицине перевешивает. Эта кнопка про смог, " +
                "а не про город",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    COFFEE(
        id = "coffee", deltaMinutes = Coefficients.COFFEE,
        label = "Кофе", emoji = "☕",
        keywords = "кофе эспрессо капучино coffee",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "2–3 чашки в день, HR 0.90",
            basis = "Таблица даёт +1 микрожизнь за 2–3 чашки, отсюда треть на чашку",
            caveat = "Наблюдательное. Кофе пьют работающие и социально активные — " +
                "часть эффекта наверняка их, а не кофе",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    VEGETABLES(
        id = "vegetables", deltaMinutes = Coefficients.VEGETABLES,
        label = "Овощи, порция", emoji = "🥦",
        keywords = "овощи салат фрукты зелень vegetables",
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposure = "≥5 порций в день, HR 0.66 у мужчин",
            basis = "Таблица даёт +4 микрожизни за ПЯТЬ порций, здесь поделено на пять",
            caveat = "Самая шаткая строка в таблице. Мерили не порции, а витамин C " +
                "в крови выше 50 нмоль/л — маркер, который заодно метит тех, кто в " +
                "целом лучше живёт. Минус треть смертности от овощей как причинный " +
                "эффект неправдоподобен",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    RED_MEAT(
        id = "red_meat", deltaMinutes = Coefficients.RED_MEAT,
        label = "Красное мясо, 85 г", emoji = "🥩",
        keywords = "мясо стейк говядина свинина meat",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Одна порция 85 г (3 унции) в день, пожизненно с 35 лет",
            basis = "Гарвардские когорты, 121 342 человека. HR 1.13 для необработанного, " +
                "1.20 для обработанного",
            caveat = "Порция меньше, чем кажется — примерно котлета для бургера. " +
                "Наблюдательное: кто ест много мяса, тот в среднем больше курит и " +
                "меньше двигается. Поправки делают, остаточное смешивание остаётся",
            sourceTitle = "Pan et al., Arch Intern Med 2012",
            sourceUrl = "https://pmc.ncbi.nlm.nih.gov/articles/PMC3712342",
        ),
    ),
    FAST_FOOD(
        id = "fast_food", deltaMinutes = Coefficients.FAST_FOOD,
        label = "Фастфуд", emoji = "🍔",
        keywords = "бургер фастфуд макдак картошка шаурма burger",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Один бургер",
            basis = "Названо в тексте статьи в одном ряду с порцией мяса и " +
                "двумя сигаретами — одна микрожизнь",
            caveat = "Отдельной строки в таблице нет, только упоминание в тексте. " +
                "Скорее всего это та же строка про красное мясо другими словами",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    TV(
        id = "tv", deltaMinutes = Coefficients.TV,
        label = "Час у экрана", emoji = "📺",
        keywords = "телевизор сериал ютуб скроллинг лежал tv",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Два часа телевизора в день, HR 1.08",
            basis = "Таблица даёт −1 микрожизнь за два часа, отсюда половина на час",
            caveat = "Мерили телевизор, а не экран вообще. Вред приписывают сидению, " +
                "так что работа за столом, вероятно, считается так же",
            sourceTitle = BMJ_TITLE, sourceUrl = BMJ,
        ),
    ),
    PET(
        id = "pet", deltaMinutes = Coefficients.PET,
        label = "С питомцем", emoji = "🐕",
        keywords = "кот собака питомец гулял с собакой pet",
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposure = "День с кошкой или собакой",
            basis = "Строка из расширенной таблицы, +1 микрожизнь",
            caveat = "В статье BMJ этой строки нет вообще, и ссылки на исследование " +
                "при ней не приведено. Доверия меньше, чем к остальным",
            sourceTitle = "Таблица микрожизней, The Conversation",
            sourceUrl = CONVERSATION,
        ),
    ),
    ACTIVE_MIND(
        id = "active_mind", deltaMinutes = Coefficients.ACTIVE_MIND,
        label = "Учился", emoji = "🧠",
        keywords = "мозг учёба чтение книга задача mind study",
        evidence = Evidence(
            confidence = Confidence.WEAK,
            exposure = "День умственной нагрузки",
            basis = "Строка из расширенной таблицы, +4 микрожизни",
            caveat = "В статье BMJ этой строки нет, ссылки на исследование не приведено. " +
                "При этом величина огромная — вторая по размеру в списке. Верить рано",
            sourceTitle = "Таблица микрожизней, The Conversation",
            sourceUrl = CONVERSATION,
        ),
    ),
    WATER(
        id = "water", deltaMinutes = Coefficients.WATER,
        label = "Стакан воды", emoji = "💧",
        keywords = "вода стакан пить гидратация water",
        evidence = Evidence(
            confidence = Confidence.NONE,
            exposure = "Натрий в крови в верхней части нормы, 11 255 человек, 30 лет",
            basis = "Ноль — это результат проверки, а не заглушка",
            caveat = "Громкое исследование про гидратацию мерило натрий в крови, " +
                "а не выпитую воду. Что «пить больше воды» продлевает жизнь — " +
                "не показано. Пей по жажде",
            sourceTitle = "Dmitrieva et al., eBioMedicine 2023",
            sourceUrl =
                "https://www.thelancet.com/journals/ebiom/article/PIIS2352-3964(22)00586-2/fulltext",
        ),
    ),
    MULTIVITAMIN(
        id = "multivitamin", deltaMinutes = Coefficients.MULTIVITAMIN,
        label = "Мультивитамины", emoji = "💊",
        keywords = "витамины бады таблетки добавки vitamin",
        evidence = Evidence(
            confidence = Confidence.NONE,
            exposure = "Ежедневный приём мультивитаминов",
            basis = "390 124 здоровых человека, более 20 лет наблюдения. Выигрыша " +
                "по смертности нет — ни общей, ни от рака, ни от сердца",
            caveat = "Это про здоровых людей и про продление жизни. При доказанном " +
                "дефиците конкретного витамина речь совсем о другом",
            sourceTitle = "Loftfield et al., JAMA Network Open 2024",
            sourceUrl = "https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2820369",
        ),
    ),
    ;

    /** Событие удлиняет жизнь. */
    val isGain: Boolean get() = deltaMinutes >= 0

    /** Величина события в микрожизнях. */
    val microlives: Double get() = deltaMinutes.toDouble() / Coefficients.MICROLIFE_MINUTES

    /** Действие проверяли, и эффекта на смертность не нашли. */
    val isNeutral: Boolean get() = deltaMinutes == 0

    /** Подходит ли тип под поисковый запрос. Регистр не важен. */
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
