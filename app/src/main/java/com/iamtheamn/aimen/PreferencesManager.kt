package com.iamtheamn.aimen

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Deux fichiers de préférences :
 * - [UI_PREFS_FILE] (en clair) : réglages d'interface non sensibles (langue, thème, couleur, voix).
 *   La langue doit rester lisible sans déchiffrement dans MainActivity.attachBaseContext.
 * - [SECURE_PREFS_FILE] (chiffré via l'Android Keystore) : adresse du serveur et identifiants Nextcloud.
 */
class PreferencesManager(context: Context) {

    private val uiPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(UI_PREFS_FILE, Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences = getSecurePrefs(context.applicationContext)

    init {
        migrateSensitiveKeys()
    }

    /**
     * Mise à jour depuis la 1.0 : les valeurs sensibles étaient stockées en clair.
     * On les déplace dans le fichier chiffré puis on les efface de l'ancien fichier.
     */
    private fun migrateSensitiveKeys() {
        val toMigrate = SENSITIVE_KEYS.filter { uiPrefs.contains(it) }
        if (toMigrate.isEmpty()) return

        val secureEditor = securePrefs.edit()
        val uiEditor = uiPrefs.edit()
        for (key in toMigrate) {
            val value = uiPrefs.getString(key, null)
            if (value != null && !securePrefs.contains(key)) secureEditor.putString(key, value)
            uiEditor.remove(key)
        }
        // commit() : on n'efface la version en clair qu'une fois la version chiffrée écrite
        if (secureEditor.commit()) uiEditor.apply()
    }

    // --- Serveur Ollama (chiffré) ---
    fun getIpAddress(): String = securePrefs.getString(KEY_IP, "") ?: ""
    fun saveIpAddress(ip: String) = securePrefs.edit().putString(KEY_IP, ip).apply()

    fun getPort(): String = securePrefs.getString(KEY_PORT, "11434") ?: "11434"
    fun savePort(port: String) = securePrefs.edit().putString(KEY_PORT, port).apply()

    // --- Interface (en clair) ---
    fun getTheme(): ThemeMode {
        val stored = uiPrefs.getString("theme", null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.DARK
    }
    fun saveTheme(theme: ThemeMode) = uiPrefs.edit().putString("theme", theme.name).apply()

    fun getLanguage(): String = uiPrefs.getString(KEY_LANGUAGE, "system") ?: "system"
    fun saveLanguage(language: String) = uiPrefs.edit().putString(KEY_LANGUAGE, language).apply()

    fun getColor(): Color = Color(uiPrefs.getInt("accent_color", DEFAULT_ACCENT_COLOR))
    fun saveColor(color: Color) = uiPrefs.edit().putInt("accent_color", color.toArgb()).apply()

    fun getIsMaleVoice(): Boolean = uiPrefs.getBoolean("tts_is_male", false)
    fun saveIsMaleVoice(isMale: Boolean) = uiPrefs.edit().putBoolean("tts_is_male", isMale).apply()

    // --- Nextcloud (chiffré) ---
    fun getNextcloudUrl(): String = securePrefs.getString(KEY_NC_URL, "") ?: ""
    fun saveNextcloudUrl(url: String) = securePrefs.edit().putString(KEY_NC_URL, url).apply()

    fun getNextcloudUser(): String = securePrefs.getString(KEY_NC_USER, "") ?: ""
    fun saveNextcloudUser(user: String) = securePrefs.edit().putString(KEY_NC_USER, user).apply()

    fun getNextcloudPassword(): String = securePrefs.getString(KEY_NC_PASSWORD, "") ?: ""
    fun saveNextcloudPassword(password: String) = securePrefs.edit().putString(KEY_NC_PASSWORD, password).apply()

    companion object {
        const val UI_PREFS_FILE = "AIMenPrefs"
        private const val SECURE_PREFS_FILE = "AIMenPrefs_secure"

        const val KEY_LANGUAGE = "app_language"
        private const val KEY_IP = "ip_address"
        private const val KEY_PORT = "port"
        private const val KEY_NC_URL = "nc_url"
        private const val KEY_NC_USER = "nc_user"
        private const val KEY_NC_PASSWORD = "nc_password"
        private val SENSITIVE_KEYS = listOf(KEY_IP, KEY_PORT, KEY_NC_URL, KEY_NC_USER, KEY_NC_PASSWORD)

        private const val DEFAULT_ACCENT_COLOR = 0xFF42A5F5.toInt()

        @Volatile
        private var secureInstance: SharedPreferences? = null

        /** Une seule instance : la création du fichier chiffré est coûteuse. */
        private fun getSecurePrefs(appContext: Context): SharedPreferences =
            secureInstance ?: synchronized(this) {
                secureInstance ?: createSecurePrefs(appContext).also { secureInstance = it }
            }

        private fun createSecurePrefs(appContext: Context): SharedPreferences =
            try {
                buildSecurePrefs(appContext)
            } catch (e: Exception) {
                // Clé Keystore invalidée (restauration, changement d'appareil...) : le fichier
                // est illisible. On repart d'un fichier vide plutôt que de planter au démarrage.
                appContext.deleteSharedPreferences(SECURE_PREFS_FILE)
                buildSecurePrefs(appContext)
            }

        private fun buildSecurePrefs(appContext: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                appContext,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
