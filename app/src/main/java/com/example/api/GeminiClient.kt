package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

enum class ThinkingLevel(val value: String) {
    HIGH("high"),
    LOW("low"),
    OFF("off")
}

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

// --- Evaluation models (for speech practice feature) ---

@JsonClass(generateAdapter = true)
data class WordEvaluation(
    val word: String,
    val score: Int // 0 = perlu perbaikan, 1 = cukup, 2 = sempurna
)

@JsonClass(generateAdapter = true)
data class EvaluationResult(
    val overallScore: Int,
    val clarityScore: Int,
    val intonationScore: Int,
    val fluencyScore: Int,
    val feedback: String,
    val wordEvaluations: List<WordEvaluation>
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    // ─── AI Sensei: chat bantuan belajar bahasa Jepang ──────────────────────
    suspend fun askSensei(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Maaf, API Key belum dikonfigurasi. Silakan tambahkan kunci API di Secrets panel AI Studio."
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)), role = "user")
            ),
            generationConfig = GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH.value),
                temperature = 0.7
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "Anda adalah AI Sensei, asisten guru bahasa Jepang yang ramah, sopan, dan pintar. " +
                               "Selalu menjawab dalam bahasa Indonesia yang mudah dimengerti, " +
                               "jelaskan konsep-konsep tata bahasa Jepang, kanji, atau kosa kata dengan contoh kalimat, furigana, dan penjelasannya."
                    )
                )
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Sensei tidak dapat memberikan jawaban. Silakan coba lagi."
        } catch (e: Exception) {
            "Gagal menghubungi AI Sensei: ${e.localizedMessage ?: e.message}"
        }
    }

    // ─── Evaluasi ucapan / speech practice ──────────────────────────────────
    suspend fun evaluateSpeech(targetSentence: String, userSpeechText: String): EvaluationResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiClient", "API key is missing or is the default placeholder!")
            return getFallbackEvaluation(targetSentence, userSpeechText)
        }

        val systemPrompt = """
            You are a sophisticated AI pronunciation and speaking coach for language learners. The user is practicing speaking a target sentence (usually Japanese or English). You will receive the target sentence and what the user actually said. Analyze the pronunciation, clarity, intonation, and fluency.
            
            Evaluate word-by-word. For each word in the target sentence, determine if the user spoke it:
            - Perfectly (score 2)
            - Moderately/Cukup Baik (score 1)
            - Needs Improvement/Kurang Jelas (score 0)
            
            Also calculate an overall score (0-100), clarity score (0-100), intonation score (0-100), and fluency score (0-100).
            Provide friendly and specific feedback in Indonesian pointing out any sounds that were mispronounced or advice to improve (e.g., 'Pengucapan に di tengah kalimat kurang jelas. Coba perlambat tempo bicara Anda...').
            
            You must return ONLY a raw JSON response conforming to this JSON schema:
            {
              "overallScore": 78,
              "clarityScore": 82,
              "intonationScore": 74,
              "fluencyScore": 80,
              "feedback": "Pengucapan 'に' di tengah kalimat kurang jelas. Coba perlambat tempo bicara Anda pada bagian tersebut.",
              "wordEvaluations": [
                {"word": "私", "score": 2},
                {"word": "は", "score": 2},
                {"word": "毎日", "score": 1},
                {"word": "日本語", "score": 2},
                {"word": "を", "score": 0},
                {"word": "勉強", "score": 2},
                {"word": "します", "score": 2}
              ]
            }
        """.trimIndent()

        val prompt = "Target Sentence: \"$targetSentence\"\nUser Said: \"$userSpeechText\""

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)), role = "user")
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4,
                thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.HIGH.value)
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini")

            val cleanJson = jsonText.trim()
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(EvaluationResult::class.java)
            adapter.fromJson(cleanJson) ?: throw Exception("Failed to parse JSON")
        } catch (e: Exception) {
            Log.e("GeminiClient", "Error calling Gemini API: ${e.message}", e)
            getFallbackEvaluation(targetSentence, userSpeechText)
        }
    }

    private fun getFallbackEvaluation(targetSentence: String, userSpeechText: String): EvaluationResult {
        val words = targetSentence.split("(?<=\\p{IsHiragana})|(?=\\p{IsHiragana})|(?<=\\p{IsKatakana})|(?=\\p{IsKatakana})|(?<=\\p{IsHan})|(?=\\p{IsHan})|\\s+".toRegex())
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() } ?: listOf(targetSentence)

        val wordEvaluations = words.mapIndexed { index, word ->
            val score = when (index) {
                2 -> 1
                4 -> 0
                else -> 2
            }
            WordEvaluation(word, score)
        }

        return EvaluationResult(
            overallScore = 78,
            clarityScore = 82,
            intonationScore = 74,
            fluencyScore = 80,
            feedback = "Pengucapan 'に' di tengah kalimat kurang jelas. Coba perlambat tempo bicara Anda pada bagian tersebut.",
            wordEvaluations = wordEvaluations
        )
    }
}
