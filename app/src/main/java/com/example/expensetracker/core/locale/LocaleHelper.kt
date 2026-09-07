package com.example.expensetracker.core.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    ARABIC("ar");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.find { it.code == code } ?: ENGLISH
    }
}

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANG = "app_language"

    fun onAttach(context: Context): Context {
        return wrap(context, getPersistedLanguage(context))
    }

    fun wrap(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getPersistedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, AppLanguage.ENGLISH.code)
            ?: AppLanguage.ENGLISH.code
    }

    fun persistLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, languageCode)
            .apply()
    }
}
