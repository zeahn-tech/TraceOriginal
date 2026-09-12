package com.example.data.api

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.generationConfig

object GeminiClient {
    private val vertexAi = Firebase.vertexAI
    
    // Using gemini-1.5-flash as it's stable and supports JSON output
    private val model = vertexAi.generativeModel(
        modelName = "gemini-1.5-flash", 
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0.2f
        }
    )

    suspend fun generateContent(prompt: String): String? {
        return try {
            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            android.util.Log.e("GeminiClient", "Error generating content: ${e.message}", e)
            null
        }
    }
}
