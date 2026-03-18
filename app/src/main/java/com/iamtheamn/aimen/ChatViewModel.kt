package com.iamtheamn.aimen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(val text: String, val isUser: Boolean)

class ChatViewModel(private val chatDao: ChatDao) : ViewModel() {
    val messages = mutableStateListOf<Message>()
    val conversations = mutableStateListOf<ConversationEntity>()
    var currentConversationId = mutableStateOf<Int?>(null)

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            val dbConvs = withContext(Dispatchers.IO) { chatDao.getAllConversations() }
            conversations.clear()
            conversations.addAll(dbConvs)

            if (conversations.isNotEmpty()) {
                selectConversation(conversations.first().id)
            }
        }
    }

    fun createNewConversation() {
        currentConversationId.value = null
        messages.clear()
    }

    fun selectConversation(conversationId: Int) {
        currentConversationId.value = conversationId
        viewModelScope.launch {
            val dbMessages = withContext(Dispatchers.IO) {
                chatDao.getMessagesForConversation(conversationId)
            }
            messages.clear()
            messages.addAll(dbMessages.map { Message(it.text, it.isUser) })
        }
    }

    fun sendMessage(userText: String, ipAddress: String) {
        if (userText.isBlank()) return

        messages.add(Message(text = userText, isUser = true))

        viewModelScope.launch(Dispatchers.IO) {
            var convId = currentConversationId.value

            if (convId == null) {
                val title = if (userText.length > 25) userText.take(25) + "..." else userText
                val newConv = ConversationEntity(title = title)
                convId = chatDao.insertConversation(newConv).toInt()

                withContext(Dispatchers.Main) {
                    currentConversationId.value = convId
                    val updatedConvs = withContext(Dispatchers.IO) { chatDao.getAllConversations() }
                    conversations.clear()
                    conversations.addAll(updatedConvs)
                }
            }

            chatDao.insertMessage(MessageEntity(conversationId = convId, text = userText, isUser = true))

            withContext(Dispatchers.Main) {
                messages.add(Message(text = "🔄 Connexion au serveur...", isUser = false))
                val messageIndex = messages.lastIndex

                viewModelScope.launch {
                    try {
                        val cleanIp = ipAddress.trim()
                        val baseUrl = "http://$cleanIp:11434/"
                        val tagsUrl = "${baseUrl}api/tags"
                        val generateUrl = "${baseUrl}api/generate"

                        messages[messageIndex] = Message(text = "🔍 Recherche de l'IA...", isUser = false)

                        val tagsResponse = RetrofitInstance.api.getModels(tagsUrl)
                        if (tagsResponse.models.isEmpty()) {
                            throw Exception("Ollama tourne, mais aucune IA n'est installée sur le Pi !")
                        }
                        val autoDetectedModel = tagsResponse.models.first().name

                        messages[messageIndex] = Message(text = "🧠 $autoDetectedModel réfléchit...", isUser = false)

                        val request = OllamaRequest(model = autoDetectedModel, prompt = userText)
                        val response = RetrofitInstance.api.generateText(generateUrl, request)

                        messages[messageIndex] = Message(text = response.response, isUser = false)

                        withContext(Dispatchers.IO) {
                            chatDao.insertMessage(MessageEntity(conversationId = convId, text = response.response, isUser = false))
                        }

                    } catch (e: Exception) {
                        messages[messageIndex] = Message(
                            text = "❌ Erreur : ${e.message ?: "Serveur injoignable"}",
                            isUser = false
                        )
                    }
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