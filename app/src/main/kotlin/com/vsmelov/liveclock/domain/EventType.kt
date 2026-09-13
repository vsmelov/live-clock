package com.vsmelov.liveclock.domain

private const val BMJ = "https://pubmed.ncbi.nlm.nih.gov/23247978/"
private const val BMJ_TITLE = "Spiegelhalter, BMJ 2012;345:e8223 — микрожизни"
private const val CONVERSATION =
    "https://cdn.theconversation.com/assets_for_articles/2017-12-22-microlife-table.pdf"

/** Общая оговорка для величин, назначенных мной, а не взятых из работы. */
private const val ESTIMATE_CAVEAT =
    "Величина назначена мной по порядку соседних строк, а не взята из работы. " +
        "Направление эффекта общепринято, размер — нет"

/**
 * Ссылка для прикидок. Это общая справка, а НЕ источник числа: числа
 * у таких строк моего изготовления, и в оговорке это написано прямо.
 */
private const val GUIDE = "https://www.health.harvard.edu/staying-healthy"

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
    /**
     * Норма за период, если у действия она есть. Пока укладываешься в норму,
     * событие стоит одно, сверх неё — другое. См. [Dosing].
     *
     * [deltaMinutes] при этом остаётся номинальной величиной для витрины;
     * настоящая цена конкретного нажатия считается в
     * [LifeState.deltaFor] и замораживается в событии.
     */
    val dosing: Dosing? = null,
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
        dosing = Dosing(DosingPeriod.DAY, normal = 1,
            withinNormalMinutes = 30, beyondNormalMinutes = Coefficients.DRINK),
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
        dosing = Dosing(DosingPeriod.DAY, normal = 3,
            withinNormalMinutes = Coefficients.COFFEE, beyondNormalMinutes = 0),
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
        dosing = Dosing(DosingPeriod.DAY, normal = 5,
            withinNormalMinutes = Coefficients.VEGETABLES, beyondNormalMinutes = 0),
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
        dosing = Dosing(DosingPeriod.WEEK, normal = 3,
            withinNormalMinutes = 0, beyondNormalMinutes = Coefficients.RED_MEAT),
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
        dosing = Dosing(DosingPeriod.WEEK, normal = 1,
            withinNormalMinutes = 0, beyondNormalMinutes = Coefficients.FAST_FOOD),
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
        dosing = Dosing(DosingPeriod.DAY, normal = 2,
            withinNormalMinutes = 0, beyondNormalMinutes = Coefficients.TV),
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
    SAUNA(
        id = "sauna", deltaMinutes = Coefficients.SAUNA,
        label = "Сауна или баня", emoji = "🧖",
        keywords = "сауна баня парная хамам sauna",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "4–7 посещений в неделю против одного, HR 0.60",
            basis = "Kuopio, 2315 мужчин, наблюдение 20.7 года",
            caveat = "HR 0.60 далеко за пределами диапазона, для которого формула " +
                "обещана (0.75–1.3), так что число завышено. В том же журнале вышел " +
                "отдельный комментарий о том, что связь может быть непричинной: " +
                "часто парятся те, кто и так здоров и не работает на трёх работах",
            sourceTitle = "Laukkanen et al., JAMA Internal Medicine 2015",
            sourceUrl =
                "https://jamanetwork.com/journals/jamainternalmedicine/article-abstract/2448449",
        ),
    ),
    NUTS(
        id = "nuts", deltaMinutes = Coefficients.NUTS,
        label = "Горсть орехов", emoji = "🥜",
        keywords = "орехи миндаль грецкие фундук nuts",
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposure = "Орехи 7+ раз в неделю против нуля, HR 0.80",
            basis = "Гарвардские когорты, 118 962 человека, чёткая доза-ответ: " +
                "реже раза в неделю 0.93, два-четыре раза 0.87, каждый день 0.80",
            caveat = "Наблюдательное, и орехи едят люди с деньгами и привычкой " +
                "следить за едой. Зато доза-ответ ровная, а это хороший признак",
            sourceTitle = "Bao et al., NEJM 2013",
            sourceUrl = "https://www.nejm.org/doi/full/10.1056/NEJMoa1307352",
        ),
        dosing = Dosing(DosingPeriod.DAY, normal = 1,
            withinNormalMinutes = Coefficients.NUTS, beyondNormalMinutes = 0),
    ),
    BIKE_COMMUTE(
        id = "bike_commute", deltaMinutes = Coefficients.BIKE_COMMUTE,
        label = "Доехал на велосипеде", emoji = "🚴",
        keywords = "велосипед вело самокат доехал bike",
        evidence = Evidence(
            confidence = Confidence.MODERATE,
            exposure = "Дорога на работу на велосипеде, HR 0.59",
            basis = "UK Biobank, 263 450 человек, 5 лет",
            caveat = "Самый крупный плюс в списке и самый подозрительный: HR 0.59 " +
                "далеко вне диапазона формулы, а на велосипеде на работу ездят " +
                "молодые, здоровые и живущие близко. Считай это верхней границей",
            sourceTitle = "Celis-Morales et al., BMJ 2017",
            sourceUrl = "https://www.ccam-tac.org/wp-content/uploads/2020/03/bmj.j1456.full_.pdf",
        ),
    ),
    RUNNING(
        id = "running", deltaMinutes = Coefficients.RUNNING,
        label = "Пробежка", emoji = "🏃",
        keywords = "бег пробежка кросс run jogging",
        evidence = Evidence(
            confidence = Confidence.STRONG,
            exposure = "Бегуны против небегающих, HR 0.70",
            basis = "55 137 человек, 15 лет. Даже 5–10 минут в день дают тот же " +
                "эффект, что и больше — порог низкий",
            caveat = "HR 0.70 чуть вне диапазона формулы. И бегают те, кто может " +
                "бегать: больные не бегают, а умирают чаще",
            sourceTitle = "Lee et al., JACC 2014",
            sourceUrl = "https://www.jacc.org/doi/10.1016/j.jacc.2014.04.058",
        ),
    ),
    FISH(
        id = "fish", deltaMinutes = Coefficients.FISH,
        label = "Рыба, порция", emoji = "🐟",
        keywords = "рыба лосось селёдка морепродукты fish",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Порция рыбы вместо порции красного мяса",
            basis = "Замена красного мяса рыбой связана со снижением смертности " +
                "во всех крупных когортах. Величина взята небольшой положительной",
            caveat = ESTIMATE_CAVEAT,
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
    ),
    SUGARY_DRINK(
        id = "sugary_drink", deltaMinutes = Coefficients.SUGARY_DRINK,
        label = "Сладкая газировка", emoji = "🥤",
        keywords = "газировка кола сок сахар лимонад soda",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Сверх двух банок в неделю",
            basis = "Сладкие напитки устойчиво связаны с диабетом и смертностью. " +
                "Величина взята чуть меньше порции красного мяса",
            caveat = ESTIMATE_CAVEAT,
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
        dosing = Dosing(DosingPeriod.WEEK, normal = 2,
            withinNormalMinutes = 0, beyondNormalMinutes = Coefficients.SUGARY_DRINK),
    ),
    SWEETS(
        id = "sweets", deltaMinutes = Coefficients.SWEETS,
        label = "Сладкое", emoji = "🍬",
        keywords = "сладкое десерт шоколад печенье торт sweets",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Сверх одного десерта в день",
            basis = "Величина взята небольшой: один десерт в день — это фон, " +
                "а не фактор риска",
            caveat = ESTIMATE_CAVEAT,
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
        dosing = Dosing(DosingPeriod.DAY, normal = 1,
            withinNormalMinutes = 0, beyondNormalMinutes = Coefficients.SWEETS),
    ),
    STRESS(
        id = "stress", deltaMinutes = Coefficients.STRESS,
        label = "Тяжёлый стресс", emoji = "😤",
        keywords = "стресс нервы конфликт ссора аврал stress",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "День сильного стресса или конфликта",
            basis = "Хронический стресс связан со смертностью через сердце и " +
                "поведение. Разовый день — величина взята небольшой",
            caveat = ESTIMATE_CAVEAT + ". И разовый стресс с хроническим " +
                "смешивать нельзя: вредит второй",
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
    ),
    READING(
        id = "reading", deltaMinutes = Coefficients.READING,
        label = "Полчаса книги", emoji = "📖",
        keywords = "чтение книга читал reading book",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Полчаса чтения книг",
            basis = "Есть известная работа Йельского университета про читающих " +
                "книги, но величина здесь назначена мной по порядку соседних строк",
            caveat = ESTIMATE_CAVEAT + ". Читают книги люди с образованием " +
                "и достатком — эффект почти наверняка не от самого чтения",
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
    ),
    FLOSS(
        id = "floss", deltaMinutes = Coefficients.FLOSS,
        label = "Зубная нить", emoji = "🦷",
        keywords = "нить флосс зубы гигиена floss",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Чистка межзубных промежутков за день",
            basis = "Болезни дёсен связаны с сердечно-сосудистой смертностью. " +
                "Величина взята маленькой",
            caveat = ESTIMATE_CAVEAT + ". Причинность тут спорна даже сильнее " +
                "обычного: за зубами следят те, кто следит и за остальным",
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
    ),
    TEA(
        id = "tea", deltaMinutes = Coefficients.TEA,
        label = "Чай", emoji = "🫖",
        keywords = "чай зелёный чёрный tea",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Чашка чая",
            basis = "По аналогии с кофе, но осторожнее: доказательная база " +
                "слабее. Взята треть от кофейной величины",
            caveat = ESTIMATE_CAVEAT,
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
    ),
    NAP(
        id = "nap", deltaMinutes = Coefficients.NAP,
        label = "Дневной сон", emoji = "😴",
        keywords = "дремота сон днём подремал nap",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Короткий сон днём",
            basis = "Короткий дневной сон обычно связывают с пользой, длинный — " +
                "с вредом. Величина взята символической",
            caveat = ESTIMATE_CAVEAT + ". Длинный дневной сон в работах уходит " +
                "в минус, и это скорее признак болезни, чем её причина",
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
        ),
    ),
    FLU_SHOT(
        id = "flu_shot", deltaMinutes = Coefficients.FLU_SHOT,
        label = "Привился от гриппа", emoji = "💉",
        keywords = "прививка вакцина грипп укол flu vaccine",
        evidence = Evidence(
            confidence = Confidence.ESTIMATE,
            exposure = "Сезонная прививка",
            basis = "Одна из немногих строк здесь, где есть рандомизированные " +
                "испытания, а не только наблюдения. Но перевод в минуты жизни " +
                "для здорового человека 30 лет — моя прикидка",
            caveat = ESTIMATE_CAVEAT + ". Основная польза приходится на пожилых " +
                "и людей с хроническими болезнями",
            sourceTitle = "Общая справка. Число моё, не отсюда",
            sourceUrl = GUIDE,
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
