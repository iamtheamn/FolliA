package com.iamtheamn.aimen

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AIMenPrefs", Context.MODE_PRIVATE)

    fun getIpAddress(): String = sharedPreferences.getString("ip_address", "") ?: ""
    fun saveIpAddress(ip: String) = sharedPreferences.edit().putString("ip_address", ip).apply()

    fun getPort(): String = sharedPreferences.getString("port", "11434") ?: "11434"
    fun savePort(port: String) = sharedPreferences.edit().putString("port", port).apply()

    fun getTheme(): ThemeMode {
        val themeString = sharedPreferences.getString("theme", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return ThemeMode.valueOf(themeString)
    }
    fun saveTheme(theme: ThemeMode) = sharedPreferences.edit().putString("theme", theme.name).apply()

    fun getLanguage(): String = sharedPreferences.getString("app_language", "system") ?: "system"
    fun saveLanguage(language: String) = sharedPreferences.edit().putString("app_language", language).apply()

    fun getColor(): Color {
        val colorInt = sharedPreferences.getInt("accent_color", android.graphics.Color.parseColor("#42A5F5"))
        return Color(colorInt)
    }
    fun saveColor(color: Color) = sharedPreferences.edit().putInt("accent_color", color.toArgb()).apply()

    fun getIsMaleVoice(): Boolean = sharedPreferences.getBoolean("tts_is_male", false)
    fun saveIsMaleVoice(isMale: Boolean) = sharedPreferences.edit().putBoolean("tts_is_male", isMale).apply()

    fun getNextcloudUrl(): String = sharedPreferences.getString("nc_url", "") ?: ""
    fun saveNextcloudUrl(url: String) = sharedPreferences.edit().putString("nc_url", url).apply()

    fun getNextcloudUser(): String = sharedPreferences.getString("nc_user", "") ?: ""
    fun saveNextcloudUser(user: String) = sharedPreferences.edit().putString("nc_user", user).apply()

    fun getNextcloudPassword(): String = sharedPreferences.getString("nc_password", "") ?: ""
    fun saveNextcloudPassword(password: String) = sharedPreferences.edit().putString("nc_password", password).apply()
}