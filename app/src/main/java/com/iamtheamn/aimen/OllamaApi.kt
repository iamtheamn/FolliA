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

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
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