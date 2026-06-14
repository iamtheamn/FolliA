package com.iamtheamn.aimen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(val text: String, val isUser: Boolean)

class ChatViewModel(
    private val chatDao: ChatDao,
    private val ttsManager: TtsManager
) : ViewModel() {
    val messages = mutableStateListOf<Message>()
    val conversations = mutableStateListOf<ConversationEntity>()
    var currentConversationId = mutableStateOf<Int?>(null)
    val availableModels = mutableStateListOf<String>()
    var selectedModel = mutableStateOf("")

    val isTtsEnabled = mutableStateOf(false)
    var isAiResponding = mutableStateOf(false)

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbConvs = chatDao.getAllConversations()
            withContext(Dispatchers.Main) {
                conversations.clear()
                conversations.addAll(dbConvs)
                if (currentConversationId.value == null && conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
                }
            }
        }
    }

    private suspend fun reloadConversations() {
        val dbConvs = chatDao.getAllConversations()
        withContext(Dispatchers.Main) {
            conversations.clear()
            conversations.addAll(dbConvs)
        }
    }

    fun stopAllAudio() {
        ttsManager.stop()
    }

    fun isTtsSpeaking(): Boolean {
        return ttsManager.isSpeaking()
    }

    fun createNewConversation() {
        currentConversationId.value = null
        messages.clear()
    }

    fun selectConversation(conversationId: Int) {
        currentConversationId.value = conversationId
        viewModelScope.launch(Dispatchers.IO) {
            val dbMessages = chatDao.getMessagesForConversation(conversationId)
            withContext(Dispatchers.Main) {
                messages.clear()
                messages.addAll(dbMessages.map { Message(it.text, it.isUser) })
            }
        }
    }

    fun renameConversation(conversationId: Int, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateConversationTitle(conversationId, newTitle)
            reloadConversations()
        }
    }

    fun togglePinConversation(conversationId: Int, currentIsPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateConversationPinned(conversationId, !currentIsPinned)
            reloadConversations()
        }
    }

    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessagesForConversation(conversationId)
            chatDao.deleteConversation(conversationId)
            val dbConvs = chatDao.getAllConversations()
            withContext(Dispatchers.Main) {
                conversations.clear()
                conversations.addAll(dbConvs)
                if (currentConversationId.value == conversationId) {
                    if (conversations.isNotEmpty()) {
                        selectConversation(conversations.first().id)
                    } else {
                        createNewConversation()
                    }
                }
            }
        }
    }

    fun fetchModels(ipAddress: String, port: String) {
        if (ipAddress.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanIp = ipAddress.trim()
                val cleanPort = port.trim().ifBlank { "11434" }
                val formattedIp = if (cleanIp.contains(":")) "[$cleanIp]" else cleanIp
                val tagsUrl = "http://$formattedIp:$cleanPort/api/tags"

                val tagsResponse = RetrofitInstance.api.getModels(tagsUrl)
                val modelNames = tagsResponse.models.map { it.name }

                withContext(Dispatchers.Main) {
                    availableModels.clear()
                    availableModels.addAll(modelNames)
                    if (modelNames.isNotEmpty() && selectedModel.value.isBlank()) {
                        selectedModel.value = modelNames.first()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(userText: String, ipAddress: String, port: String) {
        if (userText.isBlank()) return

        isAiResponding.value = true
        ttsManager.stop()

        messages.add(Message(text = userText, isUser = true))
        messages.add(Message(text = "...", isUser = false))
        val messageIndex = messages.lastIndex

        backgroundScope.launch {
            var convId = currentConversationId.value
            var fullText = ""
            var spokenTextLength = 0

            try {
                if (convId == null) {
                    val title = if (userText.length > 25) userText.take(25) + "..." else userText
                    val newConv = ConversationEntity(title = title)
                    convId = chatDao.insertConversation(newConv).toInt()
                    withContext(Dispatchers.Main) {
                        currentConversationId.value = convId
                        reloadConversations()
                    }
                }

                chatDao.insertMessage(MessageEntity(conversationId = convId, text = userText, isUser = true))

                val cleanIp = ipAddress.trim()
                val cleanPort = port.trim().ifBlank { "11434" }
                val formattedIp = if (cleanIp.contains(":")) "[$cleanIp]" else cleanIp
                val baseUrl = "http://$formattedIp:$cleanPort/"
                val chatUrl = "${baseUrl}api/chat"
                val targetModel = selectedModel.value.ifBlank { "llama3" }

                val chatHistory = messages.take(messageIndex).filter {
                    !it.text.startsWith("❌ Erreur") && !it.text.startsWith("[Erreur")
                }.map {
                    OllamaChatMessage(role = if (it.isUser) "user" else "assistant", content = it.text)
                }

                val request = OllamaRequest(model = targetModel, messages = chatHistory, stream = true)
                val responseBody = RetrofitInstance.api.generateTextStream(chatUrl, request)

                val gson = Gson()

                responseBody.charStream().buffered().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        try {
                            val chunk = gson.fromJson(line, OllamaResponse::class.java)
                            val token = chunk.message?.content ?: chunk.response ?: ""
                            fullText += token

                            if (isTtsEnabled.value) {
                                val unprocessedText = fullText.substring(spokenTextLength)
                                val lastPunctuationIndex = unprocessedText.indexOfLast { it in ".!?\n,:" }

                                if (lastPunctuationIndex != -1) {
                                    val chunkToSpeak = unprocessedText.substring(0, lastPunctuationIndex + 1)
                                    ttsManager.speakChunk(chunkToSpeak, fullText)
                                    spokenTextLength += chunkToSpeak.length
                                }
                            }

                            withContext(Dispatchers.Main) {
                                if (messages.size > messageIndex) {
                                    messages[messageIndex] = Message(text = fullText, isUser = false)
                                }
                            }
                        } catch (e: Exception) {
                        }
                        line = reader.readLine()
                    }

                    if (isTtsEnabled.value && spokenTextLength < fullText.length) {
                        val remaining = fullText.substring(spokenTextLength)
                        ttsManager.speakChunk(remaining, fullText)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (messages.size > messageIndex) {
                        messages[messageIndex] = Message(
                            text = fullText + "\n[Erreur ou coupure : ${e.message}]",
                            isUser = false
                        )
                    }
                }
            } finally {
                isAiResponding.value = false
                withContext(NonCancellable) {
                    if (fullText.isNotBlank() && convId != null) {
                        chatDao.insertMessage(MessageEntity(conversationId = convId, text = fullText, isUser = false))
                    }
                }
            }
        }
    }
}

class ChatViewModelFactory(
    private val chatDao: ChatDao,
    private val ttsManager: TtsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatDao, ttsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}