# -*- coding: utf-8 -*-
"""Generates string resources for both locales and EventType.kt from one table."""
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from actions import ACTIONS

ROOT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main"

def esc(text):
    out = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return out.replace("'", "\\'").replace('"', '\\"')

# Interface chrome: key -> (english, russian)
CHROME = {
 "app_name": ("Live Clock", "Live Clock"),
 "widget_description": ("Life remaining and quick action buttons", "Остаток жизни и кнопки быстрых действий"),
 "years_suffix": ("years", "лет"),
 "widget_days_suffix": ("d", "д"),
 "widget_today_suffix": ("today", "сегодня"),
 "remaining_title": ("Remaining", "Осталось"),
 "expected_moment": ("Expected moment: %1$s", "Ожидаемый момент: %1$s"),
 "actions_title": ("Actions", "Действия"),
 "actions_search": ("Search actions", "Поиск действия"),
 "actions_nothing_found": ("Nothing found for «%1$s»", "Ничего не нашлось по запросу «%1$s»"),
 "actions_used_times": ("%1$d times", "%1$d раз"),
 "actions_never_used": ("never", "ни разу"),
 "minutes_delta": ("%1$s min", "%1$s мин"),
 "pinned_title": ("On the widget", "На виджете"),
 "pinned_empty": ("Nothing pinned — the widget will show default buttons",
                  "Ничего не закреплено — на виджете будут кнопки по умолчанию"),
 "pinned_hint": ("The star pins an action to the widget, the arrows reorder the buttons",
                 "Звезда закрепляет действие на виджете, стрелки меняют порядок кнопок"),
 "norm_left": ("norm %1$s: %2$d of %3$d left", "норма %1$s: осталось %2$d из %3$d"),
 "norm_exceeded": ("norm %1$s used up (%2$d of %3$d)", "норма %1$s исчерпана (%2$d из %3$d)"),
 "evidence_norm": ("Norm", "Норма"),
 "evidence_norm_body": ("Within the norm (%1$d %2$s) an event costs %3$s min, beyond it %4$s min",
                        "В пределах нормы (%1$d %2$s) событие стоит %3$s мин, сверх нормы — %4$s мин"),
 "evidence_exposure": ("What was measured", "Что мерили"),
 "evidence_basis": ("How the number was derived", "Как получено число"),
 "evidence_caveat": ("What not to trust", "Чему не верить"),
 "evidence_source": ("Source", "Источник"),
 "evidence_open_source": ("Open source", "Открыть источник"),
 "evidence_close": ("Close", "Закрыть"),
 "evidence_value": ("Value: %1$s min", "Величина: %1$s мин"),
 "today_title": ("Today", "Сегодня"),
 "today_empty": ("Nothing logged today yet", "Сегодня пока пусто"),
 "today_total": ("Day total: %1$s min", "Итог за день: %1$s мин"),
 "undo_last": ("Undo last", "Отменить последнее"),
 "settings_title": ("Settings", "Настройки"),
 "birth_date_label": ("Date of birth", "Дата рождения"),
 "base_expectancy_label": ("Base life expectancy, years", "Базовая продолжительность, лет"),
 "base_expectancy_invalid": ("A positive number is required, for example 80",
                             "Нужно положительное число, например 80"),
 "sync_title": ("Sync", "Синхронизация"),
 "sync_enabled_label": ("Upload events to a server", "Выгружать события на сервер"),
 "sync_url_label": ("URL (https only)", "URL (только https)"),
 "sync_token_label": ("Bearer token", "Bearer-токен"),
 "sync_hint": ("Off by default. The app works fully offline: the source of truth is the database on the device, and the server never overwrites it",
               "Выключено по умолчанию. Приложение полностью работает офлайн: источник правды — база на устройстве, сервер её не переписывает"),
 "dialog_ok": ("OK", "Ок"),
 "dialog_cancel": ("Cancel", "Отмена"),
 # language
 "language_title": ("Language", "Язык"),
 "language_system": ("System", "Системный"),
 "language_english": ("English", "English"),
 "language_russian": ("Русский", "Русский"),
 # confidence
 "confidence_strong": ("Solid", "Надёжно"),
 "confidence_moderate": ("Moderate", "Средне"),
 "confidence_weak": ("Weak", "Слабо"),
 "confidence_none": ("No effect", "Эффекта нет"),
 "confidence_estimate": ("Estimate", "Прикидка"),
 "confidence_chosen": ("Your value", "Твоё значение"),
 # dosing periods
 "period_day": ("per day", "за день"),
 "period_week": ("per week", "за неделю"),
 # streaks and weekly summary
 "streaks_title": ("Streaks", "Серии"),
 "streaks_empty": ("No streaks yet — they start the day after a clean day", "Серий пока нет — они начинаются со следующего чистого дня"),
 "streak_days": ("%1$d days in a row", "%1$d дней подряд"),
 "streak_clean_of": ("clean of %1$s", "без: %1$s"),
 "week_title": ("This week", "За неделю"),
 "week_total": ("Week total: %1$s min", "Итог за неделю: %1$s мин"),
 "week_best": ("Biggest gain: %1$s", "Больше всего дал: %1$s"),
 "week_worst": ("Biggest loss: %1$s", "Больше всего забрал: %1$s"),
 "week_events": ("%1$d events", "%1$d событий"),
 # export
 "export_title": ("Backup", "Резервная копия"),
 "export_hint": ("Everything lives only in this phone's memory. Export writes the whole log to a file you keep",
                 "Всё живёт только в памяти телефона. Экспорт записывает весь лог в файл, который останется у тебя"),
 "export_button": ("Export log to a file", "Выгрузить лог в файл"),
 "export_done": ("Saved", "Сохранено"),
 "export_failed": ("Could not save the file", "Не удалось сохранить файл"),
 "import_button": ("Restore from a file", "Восстановить из файла"),
 "import_done": ("Restored: %1$d events", "Восстановлено: %1$d событий"),
 "import_failed": ("Could not read the file", "Не удалось прочитать файл"),
}

def write_strings(path, index):
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             "<!-- Generated from scratchpad/actions.py. Edit the table, not this file. -->",
             "<resources>"]
    for key, pair in CHROME.items():
        lines.append('    <string name="%s">%s</string>' % (key, esc(pair[index])))
    lines.append("")
    for a in ACTIONS:
        lines.append("    <!-- %s -->" % a["id"])
        for field in ("label", "exposure", "basis", "caveat"):
            lines.append('    <string name="action_%s_%s">%s</string>'
                         % (a["id"], field, esc(a[field][index])))
        if index == 0:
            # Search terms carry both languages and are deliberately NOT
            # localised: typing in either language must find the action
            # whatever the interface is set to.
            lines.append('    <string name="action_%s_keywords" translatable="false">%s</string>'
                         % (a["id"], esc(a["kw"])))
    lines.append("</resources>")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")

write_strings(ROOT / "res/values/strings.xml", 0)
write_strings(ROOT / "res/values-ru/strings.xml", 1)
print("strings: en + ru")
