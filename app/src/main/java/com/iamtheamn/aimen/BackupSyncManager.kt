package com.iamtheamn.aimen

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class BackupData(
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>
)

class BackupSyncManager(private val chatDao: ChatDao, private val prefs: PreferencesManager) {

    suspend fun getBackupJsonString(): String = withContext(Dispatchers.IO) {
        val allConversations = chatDao.getAllConversations()
        val allMessages = mutableListOf<MessageEntity>()
        for (conv in allConversations) {
            allMessages.addAll(chatDao.getMessagesForConversation(conv.id))
        }
        Gson().toJson(BackupData(allConversations, allMessages))
    }

    suspend fun restoreFromJsonString(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData = Gson().fromJson(jsonString, BackupData::class.java)

            for (conv in chatDao.getAllConversations()) {
                chatDao.deleteConversation(conv.id)
                chatDao.deleteMessagesForConversation(conv.id)
            }

            for (conv in backupData.conversations) {
                chatDao.insertConversation(conv)
            }
            for (msg in backupData.messages) {
                chatDao.insertMessage(msg)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun backupToNextcloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonData = getBackupJsonString()

            var rawUrl = prefs.getNextcloudUrl().trim()
            if (!rawUrl.endsWith("/")) rawUrl += "/"
            val targetUrl = URL("${rawUrl}remote.php/webdav/follia_backup.json")

            val connection = targetUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true

            val auth = "${prefs.getNextcloudUser()}:${prefs.getNextcloudPassword()}"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic ${encodedAuth.trim()}")
            connection.setRequestProperty("Content-Type", "application/json")

            val outStream = OutputStreamWriter(connection.outputStream)
            outStream.write(jsonData)
            outStream.close()

            return@withContext connection.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun restoreFromNextcloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            var rawUrl = prefs.getNextcloudUrl().trim()
            if (!rawUrl.endsWith("/")) rawUrl += "/"
            val targetUrl = URL("${rawUrl}remote.php/webdav/follia_backup.json")

            val connection = targetUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val auth = "${prefs.getNextcloudUser()}:${prefs.getNextcloudPassword()}"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic ${encodedAuth.trim()}")

            if (connection.responseCode in 200..299) {
                val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext restoreFromJsonString(jsonResponse)
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}