package com.industrialvision.ai.llm

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anthropic API Client for Claude integration
 *
 * Provides:
 * - Text completion with Claude models
 * - Vision capabilities with Claude's multimodal support
 * - Streaming responses
 * - Retry logic with exponential backoff
 */
@Singleton
class AnthropicClient @Inject constructor(
    private val config: LLMConfiguration
) {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", config.apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1"
        private const val MESSAGES_ENDPOINT = "$BASE_URL/messages"
    }

    /**
     * Complete a text prompt using Claude
     */
    suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        val request = MessageRequest(
            model = config.model,
            maxTokens = config.maxTokens,
            messages = listOf(
                Message(role = "user", content = listOf(TextContent(text = prompt)))
            ),
            temperature = config.temperature
        )

        executeRequest(request)
    }

    /**
     * Complete with system prompt
     */
    suspend fun completeWithSystem(
        systemPrompt: String,
        userPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val request = MessageRequest(
            model = config.model,
            maxTokens = config.maxTokens,
            system = systemPrompt,
            messages = listOf(
                Message(role = "user", content = listOf(TextContent(text = userPrompt)))
            ),
            temperature = config.temperature
        )

        executeRequest(request)
    }

    /**
     * Complete with vision - analyze an image
     */
    suspend fun completeWithVision(
        prompt: String,
        imageBase64: String,
        imageMediaType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        val request = MessageRequest(
            model = config.model,
            maxTokens = config.maxTokens,
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        ImageContent(
                            source = ImageSource(
                                type = "base64",
                                mediaType = imageMediaType,
                                data = imageBase64
                            )
                        ),
                        TextContent(text = prompt)
                    )
                )
            ),
            temperature = config.temperature
        )

        executeRequest(request)
    }

    /**
     * Stream completion responses
     */
    fun streamComplete(
        systemPrompt: String,
        messages: List<ChatMessage>
    ): Flow<String> = flow {
        val request = MessageRequest(
            model = config.model,
            maxTokens = config.maxTokens,
            system = systemPrompt,
            messages = messages.map { msg ->
                Message(
                    role = msg.role,
                    content = listOf(TextContent(text = msg.content))
                )
            },
            temperature = config.temperature,
            stream = true
        )

        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(MESSAGES_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API error: ${response.code} - ${response.body?.string()}")
            }

            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue

                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ")
                        if (data == "[DONE]") break

                        try {
                            val event = gson.fromJson(data, StreamEvent::class.java)
                            if (event.type == "content_block_delta") {
                                event.delta?.text?.let { emit(it) }
                            }
                        } catch (e: Exception) {
                            // Continue on parse errors
                        }
                    }
                }
            }
        }
    }

    /**
     * Multi-turn conversation
     */
    suspend fun converse(
        conversationHistory: List<ChatMessage>,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val request = MessageRequest(
            model = config.model,
            maxTokens = config.maxTokens,
            system = systemPrompt,
            messages = conversationHistory.map { msg ->
                Message(
                    role = msg.role,
                    content = listOf(TextContent(text = msg.content))
                )
            },
            temperature = config.temperature
        )

        executeRequest(request)
    }

    private suspend fun executeRequest(request: MessageRequest): String {
        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(MESSAGES_ENDPOINT)
            .post(requestBody)
            .build()

        return withRetry(maxRetries = 3) {
            client.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    throw IOException("API error: ${response.code} - $responseBody")
                }

                val messageResponse = gson.fromJson(responseBody, MessageResponse::class.java)
                messageResponse.content
                    .filterIsInstance<TextContentResponse>()
                    .joinToString("") { it.text }
            }
        }
    }

    private suspend fun <T> withRetry(
        maxRetries: Int,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxRetries - 1) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                if (e.message?.contains("429") == true || e.message?.contains("529") == true) {
                    // Rate limited or overloaded - wait longer
                    kotlinx.coroutines.delay(currentDelay * 2)
                } else {
                    kotlinx.coroutines.delay(currentDelay)
                }
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        return block() // Last attempt
    }
}

// Request/Response Data Classes

data class MessageRequest(
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val messages: List<Message>,
    val system: String? = null,
    val temperature: Float = 0.7f,
    val stream: Boolean = false
)

data class Message(
    val role: String,
    val content: List<Content>
)

sealed class Content

data class TextContent(
    val type: String = "text",
    val text: String
) : Content()

data class ImageContent(
    val type: String = "image",
    val source: ImageSource
) : Content()

data class ImageSource(
    val type: String,
    @SerializedName("media_type")
    val mediaType: String,
    val data: String
)

data class MessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentResponse>,
    val model: String,
    @SerializedName("stop_reason")
    val stopReason: String?,
    val usage: Usage?
)

sealed class ContentResponse

data class TextContentResponse(
    val type: String = "text",
    val text: String
) : ContentResponse()

data class Usage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int
)

data class StreamEvent(
    val type: String,
    val index: Int? = null,
    val delta: Delta? = null
)

data class Delta(
    val type: String? = null,
    val text: String? = null
)

/**
 * OpenAI Client for GPT models (alternative provider)
 */
@Singleton
class OpenAIClient @Inject constructor(
    private val config: LLMConfiguration
) {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        private const val BASE_URL = "https://api.openai.com/v1"
        private const val COMPLETIONS_ENDPOINT = "$BASE_URL/chat/completions"
    }

    suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        val request = OpenAIRequest(
            model = "gpt-4-turbo-preview",
            messages = listOf(
                OpenAIMessage(role = "user", content = prompt)
            ),
            maxTokens = config.maxTokens,
            temperature = config.temperature
        )

        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(COMPLETIONS_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                throw IOException("OpenAI API error: ${response.code} - $responseBody")
            }

            val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
            openAIResponse.choices.firstOrNull()?.message?.content ?: ""
        }
    }
}

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val temperature: Float
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage?
)

data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class OpenAIUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
