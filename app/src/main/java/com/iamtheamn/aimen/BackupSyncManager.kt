package com.iamtheamn.aimen

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class BackupData(
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>
)

class BackupSyncManager(private val chatDao: ChatDao, private val prefs: PreferencesManager) {

    suspend fun getBackupJsonString(): String = withContext(Dispatchers.IO) {
        val allConversations = chatDao.getAllConversations()
        val allMessages = allConversations.flatMap { chatDao.getMessagesForConversation(it.id) }
        Gson().toJson(BackupData(allConversations, allMessages))
    }

    suspend fun restoreFromJsonString(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData: BackupData? = Gson().fromJson(jsonString, BackupData::class.java)

            // Gson ne respecte pas la non-nullabilité Kotlin : un JSON incomplet donne des
            // champs null. On valide AVANT de toucher aux données existantes.
            @Suppress("SENSELESS_COMPARISON")
            if (backupData == null || backupData.conversations == null || backupData.messages == null) {
                return@withContext false
            }

            // Transaction : si l'insertion échoue, les anciennes conversations sont conservées
            chatDao.replaceAll(backupData.conversations, backupData.messages)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun backupToNextcloud(): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val jsonData = getBackupJsonString()
            connection = openNextcloudConnection() ?: return@withContext false
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonData) }

            connection.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun restoreFromNextcloud(): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openNextcloudConnection() ?: return@withContext false
            connection.requestMethod = "GET"

            if (connection.responseCode !in 200..299) return@withContext false

            val jsonResponse = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            restoreFromJsonString(jsonResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Ouvre une connexion authentifiée vers le fichier de sauvegarde.
     * Retourne null si l'URL n'est pas en HTTPS : les identifiants (Basic Auth)
     * ne doivent jamais circuler en clair.
     */
    private fun openNextcloudConnection(): HttpURLConnection? {
        val baseUrl = prefs.getNextcloudUrl().trim().trimEnd('/')
        if (!baseUrl.startsWith("https://", ignoreCase = true)) return null

        val connection = URL("$baseUrl/remote.php/webdav/$BACKUP_FILE_NAME").openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS

        val credentials = "${prefs.getNextcloudUser()}:${prefs.getNextcloudPassword()}"
        val encoded = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        connection.setRequestProperty("Authorization", "Basic $encoded")
        return connection
    }

    private companion object {
        const val BACKUP_FILE_NAME = "follia_backup.json"
        const val TIMEOUT_MS = 20_000
    }
}
