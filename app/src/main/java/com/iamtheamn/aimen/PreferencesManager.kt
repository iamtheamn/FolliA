package com.iamtheamn.aimen

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AIMenPrefs", Context.MODE_PRIVATE)

    fun getIpAddress(): String {
        return prefs.getString("server_ip", "") ?: ""
    }

    fun saveIpAddress(ip: String) {
        prefs.edit().putString("server_ip", ip).apply()
    }

    fun getPort(): String {
        return prefs.getString("server_port", "11434") ?: "11434"
    }

    fun savePort(port: String) {
        prefs.edit().putString("server_port", port).apply()
    }

    fun getTheme(): ThemeMode {
        val themeStr = prefs.getString("theme_mode", ThemeMode.DARK.name)
        return try {
            ThemeMode.valueOf(themeStr ?: ThemeMode.DARK.name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun saveTheme(theme: ThemeMode) {
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("app_language", "system") ?: "system"
    }

    fun saveLanguage(language: String) {
        prefs.edit().putString("app_language", language).apply()
    }

    fun getColor(): Color {
        val colorInt = prefs.getInt("accent_color_argb", 0xFF64B5F6.toInt())
        return Color(colorInt)
    }

    fun saveColor(color: Color) {
        prefs.edit().putInt("accent_color_argb", color.toArgb()).apply()
    }
}