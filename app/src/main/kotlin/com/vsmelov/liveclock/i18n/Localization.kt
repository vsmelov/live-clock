package com.vsmelov.liveclock.i18n

import android.content.Context
import android.content.res.Configuration
import com.vsmelov.liveclock.domain.AppLanguage
import java.util.Locale

/**
 * Resolves resources in the language the user picked rather than the device one.
 *
 * Done by wrapping the Context instead of using the platform's per-app language
 * API on purpose: that API only exists from API 33, and this app also has to
 * localise the widget, whose strings are resolved by us inside our own process
 * when Glance builds the RemoteViews.
 */
object Localization {

    /** A context whose resources answer in [language]. Returns [context] for SYSTEM. */
    fun contextFor(context: Context, language: AppLanguage): Context {
        val tag = language.tag ?: return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(configuration)
    }
}
