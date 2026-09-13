package com.vsmelov.liveclock.domain

import androidx.annotation.StringRes
import com.vsmelov.liveclock.R

/**
 * Interface language.
 *
 * Stored as a stable [id] rather than an ordinal so that adding a language never
 * reshuffles anyone's saved preference.
 *
 * [SYSTEM] is the default and follows the device. The other two override it,
 * which matters because a phone set to one language is not proof that its owner
 * wants the app in it.
 */
enum class AppLanguage(val id: String, @StringRes val labelRes: Int, val tag: String?) {
    SYSTEM("system", R.string.language_system, null),
    ENGLISH("en", R.string.language_english, "en"),
    RUSSIAN("ru", R.string.language_russian, "ru"),
    ;

    companion object {
        fun fromId(id: String?): AppLanguage =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
