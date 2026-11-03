package com.template.presentation.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK = "is_dark_theme"

    var isDarkTheme by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean(KEY_DARK, true)
    }

    fun toggle(context: Context) {
        isDarkTheme = !isDarkTheme
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK, isDarkTheme)
            .apply()
    }
}
