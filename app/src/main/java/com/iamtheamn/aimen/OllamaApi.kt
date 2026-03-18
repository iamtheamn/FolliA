package com.iamtheamn.aimen

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

data class OllamaResponse(
    val response: String,
    val done: Boolean
)

data class OllamaTagsResponse(val models: List<OllamaModel>)

data class OllamaModel(val name: String)

interface OllamaApiService {
    @POST
    suspend fun generateText(@Url url: String, @Body request: OllamaRequest): OllamaResponse

    @GET
    suspend fun getModels(@Url url: String): OllamaTagsResponse
}

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.MINUTES)
        .readTimeout(3, TimeUnit.MINUTES)
        .writeTimeout(3, TimeUnit.MINUTES)
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