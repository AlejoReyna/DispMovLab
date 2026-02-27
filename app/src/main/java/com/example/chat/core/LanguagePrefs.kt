package com.example.chat.core

import android.content.Context

object LanguagePrefs {
    private const val PREF_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    const val LANG_ES = "es"
    const val LANG_EN = "en"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_ES) ?: LANG_ES
    }

    fun setLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
}
