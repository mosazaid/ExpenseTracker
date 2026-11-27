package com.mousa.mizani.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    ARABIC("ar")
}

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANG = "app_language"

    /** Applies the chosen language. Without a choice the context keeps following the phone. */
    fun onAttach(context: Context): Context {
        val chosen = chosenLanguage(savedLanguage(context)) ?: return context
        return wrap(context, chosen.code)
    }

    /** The language the user picked, or null if [savedCode] holds no supported choice. */
    fun chosenLanguage(savedCode: String?): AppLanguage? =
        AppLanguage.entries.find { it.code == savedCode }

    /** The language the UI is shown in. Without a choice, only an Arabic phone gets Arabic. */
    fun shownLanguage(savedCode: String?, phoneLanguage: String): AppLanguage =
        chosenLanguage(savedCode)
            ?: chosenLanguage(phoneLanguage)
            ?: AppLanguage.ENGLISH

    /** Read from the system resources, which the in-app choice never overrides. */
    fun phoneLanguage(): String = Resources.getSystem().configuration.locales[0].language

    fun wrap(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        // Override only the language, so the rest keeps following the device.
        val overrides = Configuration().apply {
            // A new Configuration has fontScale 1, which would reset the user's font size.
            fontScale = 0f
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(overrides)
    }

    fun savedLanguage(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)

    fun persistLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, languageCode)
            .apply()
    }
}
