package com.iamtheamn.aimen

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class OllamaRequest(
    val model: String,
    val prompt: String? = null,
    val messages: List<OllamaChatMessage>? = null,
    val stream: Boolean = false
)

data class OllamaChatMessage(
    val role: String,
    val content: String
)

data class OllamaResponse(
    val model: String?,
    val created_at: String?,
    val response: String?,
    val message: OllamaChatMessage?,
    val done: Boolean?
)

data class OllamaTagsResponse(
    val models: List<OllamaModel>
)

data class OllamaModel(
    val name: String
)

interface OllamaApiService {
    @GET
    suspend fun getModels(@Url url: String): OllamaTagsResponse

    @Streaming
    @POST
    suspend fun generateTextStream(@Url url: String, @Body request: OllamaRequest): ResponseBody
}

/**
 * Construit l'URL de base du serveur Ollama.
 * HTTP par défaut (réseau local / VPN) ; HTTPS si l'adresse saisie commence par "https://".
 */
fun buildOllamaBaseUrl(address: String, port: String): String {
    val trimmed = address.trim().trimEnd('/')
    val scheme = if (trimmed.startsWith("https://", ignoreCase = true)) "https" else "http"
    val host = trimmed.substringAfter("://")
    val cleanPort = port.trim().ifBlank { "11434" }
    // Une IPv6 littérale doit être entourée de crochets dans une URL
    val formattedHost = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
    return "$scheme://$formattedHost:$cleanPort/"
}

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES) // un modèle local peut mettre longtemps à répondre
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: OllamaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OllamaApiService::class.java)
    }
}