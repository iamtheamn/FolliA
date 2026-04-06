package com.iamtheamn.aimen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(val text: String, val isUser: Boolean)

class ChatViewModel(private val chatDao: ChatDao) : ViewModel() {
    val messages = mutableStateListOf<Message>()
    val conversations = mutableStateListOf<ConversationEntity>()
    var currentConversationId = mutableStateOf<Int?>(null)
    val availableModels = mutableStateListOf<String>()
    var selectedModel = mutableStateOf("")

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbConvs = chatDao.getAllConversations()
            withContext(Dispatchers.Main) {
                conversations.clear()
                conversations.addAll(dbConvs)
                if (conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
                }
            }
        }
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

        messages.add(Message(text = userText, isUser = true))
        messages.add(Message(text = "...", isUser = false))
        val messageIndex = messages.lastIndex

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var convId = currentConversationId.value
                if (convId == null) {
                    val title = if (userText.length > 25) userText.take(25) + "..." else userText
                    val newConv = ConversationEntity(title = title)
                    convId = chatDao.insertConversation(newConv).toInt()
                    withContext(Dispatchers.Main) {
                        currentConversationId.value = convId
                        val updatedConvs = chatDao.getAllConversations()
                        conversations.clear()
                        conversations.addAll(updatedConvs)
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
                    !it.text.startsWith("❌ Erreur")
                }.map {
                    OllamaChatMessage(role = if (it.isUser) "user" else "assistant", content = it.text)
                }

                val request = OllamaRequest(model = targetModel, messages = chatHistory, stream = true)
                val responseBody = RetrofitInstance.api.generateTextStream(chatUrl, request)

                val gson = Gson()
                var fullText = ""

                responseBody.charStream().buffered().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        try {
                            val chunk = gson.fromJson(line, OllamaResponse::class.java)
                            val token = chunk.message?.content ?: chunk.response ?: ""
                            fullText += token

                            withContext(Dispatchers.Main) {
                                messages[messageIndex] = Message(text = fullText, isUser = false)
                            }
                        } catch (e: Exception) {
                        }
                        line = reader.readLine()
                    }
                }

                chatDao.insertMessage(MessageEntity(conversationId = convId, text = fullText, isUser = false))

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages[messageIndex] = Message(
                        text = "❌ Erreur : ${e.message ?: "Serveur injoignable"}",
                        isUser = false
                    )
                }
            }
        }
    }
}

class ChatViewModelFactory(private val chatDao: ChatDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}