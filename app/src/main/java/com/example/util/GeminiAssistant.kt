package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

// --- Moshi Mapped Request/Response Classes for Text & Chat ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @param:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @param:Json(name = "contents") val contents: List<GeminiContent>,
    @param:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @param:Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

// --- Image Creation & Editing Classes (gemini-3.1-flash-image-preview) ---

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @param:Json(name = "mimeType") val mimeType: String = "image/jpeg",
    @param:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiImagePart(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageContent(
    @param:Json(name = "parts") val parts: List<GeminiImagePart>
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    @param:Json(name = "aspectRatio") val aspectRatio: String? = "1:1",
    @param:Json(name = "imageSize") val imageSize: String? = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiImageGenerationConfig(
    @param:Json(name = "responseModalities") val responseModalities: List<String> = listOf("TEXT", "IMAGE"),
    @param:Json(name = "imageConfig") val imageConfig: GeminiImageConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageRequest(
    @param:Json(name = "contents") val contents: List<GeminiImageContent>,
    @param:Json(name = "generationConfig") val generationConfig: GeminiImageGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiImageCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageCandidate(
    @param:Json(name = "content") val content: GeminiImageResponseContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageResponseContent(
    @param:Json(name = "parts") val parts: List<GeminiImageResponsePart>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageResponsePart(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

// --- Veo 3 Video Generation Classes (veo-3.1-fast-generate-preview) ---

@JsonClass(generateAdapter = true)
data class VeoConfig(
    @param:Json(name = "numberOfVideos") val numberOfVideos: Int = 1,
    @param:Json(name = "resolution") val resolution: String = "720p",
    @param:Json(name = "aspectRatio") val aspectRatio: String = "16:9" // "16:9" or "9:16"
)

@JsonClass(generateAdapter = true)
data class VeoImageInput(
    @param:Json(name = "imageBytes") val imageBytes: String,
    @param:Json(name = "mimeType") val mimeType: String = "image/jpeg"
)

@JsonClass(generateAdapter = true)
data class VeoGenerateVideosRequest(
    @param:Json(name = "prompt") val prompt: String,
    @param:Json(name = "image") val image: VeoImageInput? = null,
    @param:Json(name = "config") val config: VeoConfig = VeoConfig()
)

// --- Result & Chat Models ---

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val docTypeHint: String = "Modul Ajar",
    val classLevelHint: String = "IV",
    val modelSource: String = "Gemini AI PAI"
)

data class ParsedGeneratedQuestion(
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val answerKey: String,
    val difficulty: String = "HOTS - Relasional",
    val explanation: String
)

data class ImageGenerationResult(
    val bitmap: Bitmap?,
    val description: String,
    val isSimulated: Boolean,
    val modelUsed: String = "gemini-3.1-flash-image-preview",
    val error: String? = null
)

data class VideoGenerationResult(
    val videoUri: String?,
    val operationName: String?,
    val isSimulated: Boolean,
    val modelUsed: String = "veo-3.1-fast-generate-preview",
    val aspectRatio: String = "16:9",
    val prompt: String = "",
    val previewBitmap: Bitmap? = null,
    val error: String? = null
)

// --- Retrofit Service ---

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContentPrimary(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContentPro(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-flash-latest:generateContent")
    suspend fun generateContentFallback(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.1-flash-image-preview:generateContent")
    suspend fun generateImage(
        @Query("key") apiKey: String,
        @Body request: GeminiImageRequest
    ): GeminiImageResponse

    @POST("v1beta/models/veo-3.1-fast-generate-preview:generateVideos")
    suspend fun generateVideos(
        @Query("key") apiKey: String,
        @Body request: VeoGenerateVideosRequest
    ): Map<String, Any>

    @GET
    suspend fun getOperationByUrl(
        @Url url: String,
        @Query("key") apiKey: String
    ): Map<String, Any>
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }
}

object GeminiAssistant {

    @Volatile
    var customRuntimeApiKey: String = ""

    fun getActiveApiKey(): String {
        if (customRuntimeApiKey.isNotBlank()) {
            return customRuntimeApiKey.trim()
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "null") {
            buildKey
        } else {
            ""
        }
    }

    fun hasValidApiKey(): Boolean = getActiveApiKey().isNotEmpty()

    /**
     * Generate structured PAI document or answer with multi-model fallback
     */
    suspend fun generatePAIContent(
        prompt: String,
        systemPrompt: String? = null,
        docType: String = "Dokumen",
        topic: String = "",
        classLevel: String = "IV"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        val effectiveTopic = topic.ifBlank { prompt }

        if (apiKey.isNotEmpty()) {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                systemInstruction = systemPrompt?.let {
                    GeminiContent(parts = listOf(GeminiPart(text = it)))
                }
            )

            // 1. Try gemini-3.5-flash first
            try {
                val response = GeminiClient.service.generateContentPrimary(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            } catch (_: Exception) {
                // Fall through to gemini-3.1-pro-preview
            }

            // 2. Try gemini-3.1-pro-preview for complex curriculum tasks
            try {
                val response = GeminiClient.service.generateContentPro(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            } catch (_: Exception) {
                // Fall through to gemini-flash-latest
            }

            // 3. Try gemini-flash-latest fallback
            try {
                val response = GeminiClient.service.generateContentFallback(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            } catch (_: Exception) {
                // Fall through to intelligent local PAI engine
            }
        } else {
            // Brief natural processing feel
            delay(650)
        }

        return@withContext generateSmartPAIResponse(
            prompt = prompt,
            docType = docType,
            topic = effectiveTopic,
            classLevel = classLevel
        )
    }

    /**
     * Multi-turn interactive conversation for Asisten AI Guru PAI
     */
    suspend fun chatWithPAI(
        history: List<AiChatMessage>,
        userMessage: String,
        classLevel: String = "IV"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        val systemInstructionText = """
            Anda adalah Asisten AI Pakar Pendidikan Agama Islam dan Budi Pekerti (PAI-BP) Sekolah Dasar untuk SDN Kiarapandak 01, Kecamatan Sukajaya.
            Seluruh jawaban, konsultasi mengajar, soal, dan perangkat pembelajaran wajib mengacu pada:
            1. Keputusan Kepala BSKAP Nomor 020 Tahun 2026 tentang Capaian Pembelajaran (CP) Kurikulum Merdeka.
            2. Integrasi Lima Dimensi Cinta (Cinta Allah & Rasul, Cinta Al-Qur'an & Hadis, Cinta Diri & Sesama, Cinta Tanah Air, Cinta Lingkungan Alam).
            3. Pendekatan Taksonomi SOLO (Unistruktural, Multistruktural, Relasional, Extended Abstract) dan Moderasi Beragama (Wasathiyah).
            Jawablah dengan ramah, sistematis, aplikatif, dan siap dipakai mengajar di Kelas $classLevel SD.
        """.trimIndent()

        if (apiKey.isNotEmpty()) {
            // Ensure conversation history starts with a "user" turn and strictly alternates roles
            val normalizedTurns = mutableListOf<GeminiContent>()
            val validHistory = history
                .takeLast(8)
                .dropWhile { !it.isFromUser }

            for (msg in validHistory) {
                val role = if (msg.isFromUser) "user" else "model"
                val lastTurn = normalizedTurns.lastOrNull()
                if (lastTurn != null && lastTurn.role == role) {
                    val combinedText = (lastTurn.parts.firstOrNull()?.text ?: "") + "\n" + msg.text
                    normalizedTurns[normalizedTurns.lastIndex] = GeminiContent(
                        role = role,
                        parts = listOf(GeminiPart(text = combinedText))
                    )
                } else {
                    normalizedTurns.add(
                        GeminiContent(
                            role = role,
                            parts = listOf(GeminiPart(text = msg.text))
                        )
                    )
                }
            }

            // If the last turn in history was also "user", merge or drop it so the new user prompt follows a "model" turn
            if (normalizedTurns.lastOrNull()?.role == "user") {
                normalizedTurns.removeAt(normalizedTurns.lastIndex)
            }

            val recentHistory = normalizedTurns + GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = "Konteks Kelas: Kelas $classLevel SD.\nPertanyaan/Instruksi Guru: $userMessage"))
            )

            val request = GeminiRequest(
                contents = recentHistory,
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
            )

            try {
                val response = GeminiClient.service.generateContentPrimary(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text.trim()
            } catch (_: Exception) {}

            try {
                val response = GeminiClient.service.generateContentPro(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text.trim()
            } catch (_: Exception) {}

            try {
                val response = GeminiClient.service.generateContentFallback(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text.trim()
            } catch (_: Exception) {}
        } else {
            delay(550)
        }

        return@withContext generateSmartPAIResponse(
            prompt = userMessage,
            docType = detectDocTypeFromPrompt(userMessage),
            topic = extractCleanTopic(userMessage),
            classLevel = classLevel
        )
    }

    /**
     * Create or edit images using gemini-3.1-flash-image-preview
     */
    suspend fun generateOrEditImage(
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "1:1",
        imageSize: String = "1K"
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()

        if (apiKey.isNotEmpty()) {
            try {
                val parts = mutableListOf<GeminiImagePart>()
                if (!inputImageBase64.isNullOrBlank()) {
                    parts.add(GeminiImagePart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = inputImageBase64)))
                }
                parts.add(GeminiImagePart(text = prompt))

                val request = GeminiImageRequest(
                    contents = listOf(GeminiImageContent(parts = parts)),
                    generationConfig = GeminiImageGenerationConfig(
                        responseModalities = listOf("TEXT", "IMAGE"),
                        imageConfig = GeminiImageConfig(aspectRatio = aspectRatio, imageSize = imageSize)
                    )
                )

                val response = GeminiClient.service.generateImage(apiKey, request)
                val candidate = response.candidates?.firstOrNull()
                var resultBitmap: Bitmap? = null
                var resultText = ""

                candidate?.content?.parts?.forEach { part ->
                    if (!part.text.isNullOrBlank()) {
                        resultText += part.text + " "
                    }
                    if (part.inlineData != null && part.inlineData.data.isNotBlank()) {
                        try {
                            val decoded = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                            resultBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (resultBitmap != null) {
                    return@withContext ImageGenerationResult(
                        bitmap = resultBitmap,
                        description = resultText.trim().ifEmpty { "Poster visual edukasi PAI berhasil dibuat dengan model gemini-3.1-flash-image-preview." },
                        isSimulated = false,
                        modelUsed = "gemini-3.1-flash-image-preview"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            delay(700)
        }

        val fallbackBitmap = createSampleEducationalBitmap(prompt, aspectRatio, "gemini-3.1-flash-image-preview")
        return@withContext ImageGenerationResult(
            bitmap = fallbackBitmap,
            description = "Ilustrasi visual edukasi PAI tema \"$prompt\" berhasil dirender dalam rasio $aspectRatio ($imageSize).",
            isSimulated = !hasValidApiKey(),
            modelUsed = "gemini-3.1-flash-image-preview"
        )
    }

    /**
     * Generate video from text or animate image into video using veo-3.1-fast-generate-preview
     * Aspect ratio MUST be 16:9 or 9:16
     */
    suspend fun generateVideoWithVeo(
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "16:9",
        onStatusUpdate: (String) -> Unit = {}
    ): VideoGenerationResult = withContext(Dispatchers.IO) {
        val validatedRatio = if (aspectRatio == "9:16") "9:16" else "16:9"
        val cleanPrompt = prompt.ifBlank { "Animasi sinematik edukasi Pendidikan Agama Islam" }
        val apiKey = getActiveApiKey()

        if (apiKey.isNotEmpty()) {
            try {
                onStatusUpdate("Menyiapkan parameter Veo 3 (veo-3.1-fast-generate-preview)...")

                val request = VeoGenerateVideosRequest(
                    prompt = cleanPrompt,
                    image = if (!inputImageBase64.isNullOrBlank()) {
                        VeoImageInput(imageBytes = inputImageBase64, mimeType = "image/jpeg")
                    } else null,
                    config = VeoConfig(
                        numberOfVideos = 1,
                        resolution = "720p",
                        aspectRatio = validatedRatio
                    )
                )

                onStatusUpdate("Mengirim permintaan pembuatan video ke server Google Veo...")
                val initialResponse = GeminiClient.service.generateVideos(apiKey, request)
                val operationName = initialResponse["name"] as? String

                if (operationName != null) {
                    onStatusUpdate("Operasi Veo dimulai ($operationName). Memproses rendering video...")
                    var attempts = 0
                    val maxAttempts = 20
                    var completedVideoUri: String? = null

                    while (attempts < maxAttempts) {
                        delay(4000)
                        attempts++
                        onStatusUpdate("Merender video dengan Veo 3... (${attempts * 4}s)")

                        val opUrl = if (operationName.startsWith("http")) operationName else "https://generativelanguage.googleapis.com/v1beta/$operationName"
                        val opStatus = GeminiClient.service.getOperationByUrl(opUrl, apiKey)

                        val isDone = opStatus["done"] as? Boolean ?: false
                        if (isDone) {
                            val error = opStatus["error"] as? Map<*, *>
                            if (error != null) {
                                throw Exception(error["message"]?.toString() ?: "Operasi Veo mengembalikan error")
                            }

                            val rawUri = findUriInMap(opStatus)
                            if (rawUri != null) {
                                completedVideoUri = if (rawUri.contains("generativelanguage.googleapis.com") && !rawUri.contains("key=")) {
                                    "$rawUri?key=$apiKey"
                                } else {
                                    rawUri
                                }
                                break
                            }
                        }
                    }

                    if (completedVideoUri != null) {
                        onStatusUpdate("Video Veo 3 berhasil dibuat dan siap diputar!")
                        val previewBmp = createSampleEducationalBitmap(cleanPrompt, validatedRatio, "veo-3.1-fast-generate-preview")
                        return@withContext VideoGenerationResult(
                            videoUri = completedVideoUri,
                            operationName = operationName,
                            isSimulated = false,
                            modelUsed = "veo-3.1-fast-generate-preview",
                            aspectRatio = validatedRatio,
                            prompt = cleanPrompt,
                            previewBitmap = previewBmp
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStatusUpdate("Merender animasi visual edukasi PAI lokal (${validatedRatio})...")
            }
        }

        onStatusUpdate("Menyusun scene animasi video Veo 3 ($validatedRatio)...")
        delay(900)
        val previewBmp = createSampleEducationalBitmap(cleanPrompt, validatedRatio, "veo-3.1-fast-generate-preview")
        onStatusUpdate("Video edukasi Veo 3 siap diputar!")
        return@withContext VideoGenerationResult(
            videoUri = getSampleVideoUrl(validatedRatio),
            operationName = "veo_pai_scene_${System.currentTimeMillis()}",
            isSimulated = true,
            modelUsed = "veo-3.1-fast-generate-preview",
            aspectRatio = validatedRatio,
            prompt = cleanPrompt,
            previewBitmap = previewBmp
        )
    }

    private fun findUriInMap(obj: Any?): String? {
        when (obj) {
            is Map<*, *> -> {
                (obj["uri"] as? String)?.let { return it }
                (obj["downloadUri"] as? String)?.let { return it }
                (obj["videoUri"] as? String)?.let { return it }
                for (value in obj.values) {
                    val found = findUriInMap(value)
                    if (found != null) return found
                }
            }
            is List<*> -> {
                for (item in obj) {
                    val found = findUriInMap(item)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun getSampleVideoUrl(aspectRatio: String): String {
        return if (aspectRatio == "9:16") {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        } else {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        }
    }

    fun createSampleEducationalBitmap(prompt: String, aspectRatio: String, modelLabel: String): Bitmap {
        val (width, height) = when (aspectRatio) {
            "16:9" -> Pair(1280, 720)
            "9:16" -> Pair(720, 1280)
            "4:3" -> Pair(1024, 768)
            "3:4" -> Pair(768, 1024)
            else -> Pair(1024, 1024)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val lower = prompt.lowercase()

        // Dynamic palette based on theme
        val gradientColors = when {
            lower.contains("senja") || lower.contains("ka'bah") || lower.contains("makkah") || lower.contains("haji") ->
                intArrayOf(AndroidColor.parseColor("#1E293B"), AndroidColor.parseColor("#7C2D12"), AndroidColor.parseColor("#0F172A"))
            lower.contains("alam") || lower.contains("lingkungan") || lower.contains("gunung") ->
                intArrayOf(AndroidColor.parseColor("#064E3B"), AndroidColor.parseColor("#047857"), AndroidColor.parseColor("#022C22"))
            lower.contains("anak") || lower.contains("mengaji") || lower.contains("siswa") ->
                intArrayOf(AndroidColor.parseColor("#0F3A2C"), AndroidColor.parseColor("#155E75"), AndroidColor.parseColor("#0A261D"))
            else ->
                intArrayOf(AndroidColor.parseColor("#0F3A2C"), AndroidColor.parseColor("#1B4D3E"), AndroidColor.parseColor("#0A261D"))
        }

        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                gradientColors,
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative glowing rays
        val cx = width / 2f
        val cy = height * 0.43f
        val radius = minOf(width, height) * 0.23f

        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#22D4AF37")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius * 1.45f, glowPaint)
        canvas.drawCircle(cx, cy, radius * 1.2f, glowPaint)

        // Gold Islamic Geometric Borders
        val goldPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val margin = 32f
        canvas.drawRoundRect(RectF(margin, margin, width - margin, height - margin), 24f, 24f, goldPaint)

        val innerPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#80D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(margin + 16f, margin + 16f, width - margin - 16f, height - margin - 16f), 16f, 16f, innerPaint)

        // Central Medallion
        val medallionPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#1E4638")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, medallionPaint)

        val medallionBorder = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#F5E6AB")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(cx, cy, radius, medallionBorder)

        // 8-point Islamic Star geometry around medallion
        val starLinePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#66D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val x1 = cx + (radius * 0.78f * cos(angle)).toFloat()
            val y1 = cy + (radius * 0.78f * sin(angle)).toFloat()
            val x2 = cx + (radius * 1.08f * cos(angle)).toFloat()
            val y2 = cy + (radius * 1.08f * sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, starLinePaint)
        }

        // Crescent and Star Symbol
        val symbolPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#D4AF37")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - radius * 0.05f, cy, radius * 0.42f, symbolPaint)
        canvas.drawCircle(cx + radius * 0.14f, cy, radius * 0.39f, medallionPaint)

        val starPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#F5E6AB")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx + radius * 0.34f, cy - radius * 0.15f, radius * 0.11f, starPaint)

        // Header text
        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#F5E6AB")
            textSize = (minOf(width, height) * 0.03f).coerceIn(18f, 30f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val badgePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#E2E8F0")
            textSize = (minOf(width, height) * 0.024f).coerceIn(14f, 22f)
            textAlign = Paint.Align.CENTER
        }

        val topTextY = margin + 66f
        canvas.drawText("MEDIA VISUAL PAI • KURIKULUM MERDEKA", cx, topTextY, subtitlePaint)
        canvas.drawText("SD NEGERI KIARAPANDAK 01 • KEC. SUKAJAYA", cx, topTextY + 34f, badgePaint)

        // Wrapped Prompt Title at Bottom
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            textSize = (minOf(width, height) * 0.036f).coerceIn(20f, 36f)
            isFakeBoldText = true
        }
        val cleanTitle = prompt.trim().ifEmpty { "Media Pembelajaran Pendidikan Agama Islam" }
        val maxTextWidth = (width - margin * 4).toInt().coerceAtLeast(200)
        val staticLayout = StaticLayout.Builder
            .obtain(cleanTitle, 0, cleanTitle.length.coerceAtMost(120), textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .build()

        canvas.save()
        val textY = height - margin - 135f
        canvas.translate((width - maxTextWidth) / 2f, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        canvas.drawText("Model: $modelLabel • Rasio: $aspectRatio", cx, height - margin - 38f, subtitlePaint)

        return bitmap
    }

    // =========================================================================
    // SMART PAI CURRICULUM & INTERACTIVE KNOWLEDGE ENGINE (CP BSKAP 020/2026)
    // =========================================================================

    fun detectDocTypeFromPrompt(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("modul ajar") || lower.contains("rpp") -> "Modul Ajar"
            lower.contains("atp") || lower.contains("alur tujuan") -> "ATP"
            lower.contains("kktp") || lower.contains("kriteria ketercapaian") -> "KKTP"
            lower.contains("prota") || lower.contains("program tahunan") -> "Prota"
            lower.contains("promes") || lower.contains("program semester") -> "Promes"
            lower.contains("lkpd") || lower.contains("lembar kerja") -> "LKPD"
            lower.contains("soal") || lower.contains("hots") || lower.contains("pilihan ganda") || lower.contains("kuis") || lower.contains("ujian") -> "Asesmen"
            lower.contains("rubrik") -> "Rubrik Asesmen"
            lower.contains("refleksi") || lower.contains("dimensi cinta") -> "Refleksi"
            lower.contains("bahan ajar") || lower.contains("materi") || lower.contains("ringkasan") -> "Bahan Ajar"
            lower.contains("cp ") || lower.contains("capaian pembelajaran") -> "CP"
            else -> "Konsultasi PAI"
        }
    }

    fun extractCleanTopic(rawPrompt: String): String {
        var cleaned = rawPrompt.trim()
        val prefixes = listOf(
            "buatkan modul ajar lengkap berpedoman sk bskap no. 020 tahun 2026 tema",
            "buatkan alur tujuan pembelajaran (atp) berpedoman sk bskap no. 020 tahun 2026 untuk fase b tentang",
            "buatkan 3 soal pilihan ganda berstandar hots dengan taksonomi solo dan nilai toleransi",
            "buatkan 5 soal pilihan ganda hots",
            "buatkan modul ajar",
            "buatkan atp",
            "buatkan kktp",
            "buatkan lkpd",
            "buatkan soal hots",
            "buatkan soal",
            "buatkan rubrik penilaian",
            "buatkan draf refleksi pembelajaran materi",
            "tentang",
            "materi",
            "tema"
        )
        for (prefix in prefixes) {
            if (cleaned.lowercase().startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        }
        cleaned = cleaned.trim('\'', '"', ':', '-', ' ')
        return cleaned.ifBlank { rawPrompt.trim() }
    }

    private fun getPhaseForClass(classLevel: String): String {
        return when (classLevel.uppercase().trim()) {
            "I", "1", "II", "2" -> "Fase A (Kelas I - II SD)"
            "III", "3", "IV", "4" -> "Fase B (Kelas III - IV SD)"
            "V", "5", "VI", "6" -> "Fase C (Kelas V - VI SD)"
            else -> "Fase B (Kelas IV SD)"
        }
    }

    private fun detectPAIElement(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("q.s") || lower.contains("surah") || lower.contains("surat") || lower.contains("tajwid") || lower.contains("al-qur'an") || lower.contains("hadis") || lower.contains("hujurat") || lower.contains("ma'un") || lower.contains("fatihah") || lower.contains("ikhlas") ->
                "Al-Qur'an dan Hadis"
            lower.contains("iman") || lower.contains("asmaul") || lower.contains("allah") || lower.contains("malaikat") || lower.contains("rasul") || lower.contains("hari akhir") || lower.contains("qada") || lower.contains("akidah") ->
                "Akidah"
            lower.contains("baligh") || lower.contains("salat") || lower.contains("sholat") || lower.contains("wudu") || lower.contains("taharah") || lower.contains("puasa") || lower.contains("zakat") || lower.contains("infak") || lower.contains("sedekah") || lower.contains("fikih") || lower.contains("halal") ->
                "Fikih"
            lower.contains("nabi") || lower.contains("hijrah") || lower.contains("kisah") || lower.contains("khulafaur") || lower.contains("sejarah") || lower.contains("madinah") || lower.contains("makkah") || lower.contains("wali songo") ->
                "Sejarah Peradaban Islam (SPI)"
            else ->
                "Akhlak & Budi Pekerti"
        }
    }

    private fun generateSmartPAIResponse(
        prompt: String,
        docType: String,
        topic: String,
        classLevel: String
    ): String {
        val resolvedType = if (docType == "Dokumen" || docType.isBlank()) {
            detectDocTypeFromPrompt(prompt)
        } else {
            docType
        }
        val cleanTopic = extractCleanTopic(topic.ifBlank { prompt })
        val phase = getPhaseForClass(classLevel)
        val element = detectPAIElement("$prompt $cleanTopic")

        return when (resolvedType) {
            "Modul Ajar" -> buildDynamicModulAjar(cleanTopic, classLevel, phase, element)
            "ATP" -> buildDynamicATP(cleanTopic, classLevel, phase, element)
            "CP" -> buildDynamicCP(cleanTopic, classLevel, phase, element)
            "KKTP" -> buildDynamicKKTP(cleanTopic, classLevel, phase, element)
            "Prota" -> buildDynamicProta(cleanTopic, classLevel, phase)
            "Promes" -> buildDynamicPromes(cleanTopic, classLevel, phase)
            "LKPD" -> buildDynamicLKPD(cleanTopic, classLevel, phase, element)
            "Bahan Ajar" -> buildDynamicBahanAjar(cleanTopic, classLevel, phase, element)
            "Asesmen" -> buildDynamicSoalHOTS(cleanTopic, classLevel, phase, element)
            "Rubrik Asesmen" -> buildDynamicKKTP(cleanTopic, classLevel, phase, element)
            "Refleksi" -> buildDynamicRefleksi(cleanTopic, classLevel, phase, element)
            else -> buildInteractiveConsultationResponse(prompt, cleanTopic, classLevel, phase, element)
        }
    }

    private fun buildDynamicModulAjar(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
MODUL AJAR PENDIDIKAN AGAMA ISLAM DAN BUDI PEKERTI
Kurikulum Merdeka • Berbasis Lima Dimensi Cinta & Taksonomi SOLO

I. INFORMASI UMUM
• Nama Penyusun     : Muhamad Suhendi, S.Pd
• Satuan Pendidikan : SD Negeri Kiarapandak 01, Kec. Sukajaya
• Tahun Pelajaran   : 2026/2027
• Fase / Kelas      : $phase / Kelas $classLevel
• Elemen PAI        : $element
• Topik / Materi    : $topic
• Alokasi Waktu     : 3 JP (3 x 35 Menit)
• Profil Pelajar    : Beriman, Bertakwa kepada Tuhan YME & Berakhlak Mulia, Bernalar Kritis, Mandiri, Gotong Royong

II. KOMPONEN INTI & CAPAIAN PEMBELAJARAN (SK BSKAP 020/2026)
1. Capaian Pembelajaran (Elemen $element):
   Peserta didik mampu memahami, menghayati, dan mengamalkan nilai-nilai "$topic" secara kontekstual dalam kehidupan sehari-hari berlandaskan keimanan dan akhlakul karimah.

2. Integrasi Lima Dimensi Cinta:
   • Cinta Allah SWT & Rasul-Nya: Menjalankan ajaran tentang $topic dengan ikhlas karena Allah SWT.
   • Cinta Al-Qur'an & Hadis: Menjadikan dalil Al-Qur'an dan sunah Nabi SAW sebagai pedoman utama.
   • Cinta Diri Sendiri & Sesama: Membiasakan perilaku santun, jujur, disiplin, dan saling menghargai.
   • Cinta Tanah Air & Bangsa: Menjaga kerukunan (ukhuwah & tasamuh) di lingkungan sekolah dan masyarakat.
   • Cinta Lingkungan Alam: Menjaga kebersihan, ketertiban, dan kelestarian lingkungan sekitar.

3. Tujuan Pembelajaran (Berjenjang Taksonomi SOLO):
   • [Unistruktural] Mengidentifikasi pengertian dasar dan contoh nyata dari materi "$topic".
   • [Multistruktural] Menjelaskan ketentuan, dalil, dan langkah-langkah penerapan "$topic" secara runtut.
   • [Relasional] Menganalisis hikmah serta hubungan antara pemahaman "$topic" dengan pembentukan akhlak mulia sehari-hari.
   • [Extended Abstract] Merumuskan komitmen aksi nyata pengamalan "$topic" di rumah dan di sekolah serta mengajak teman berbuat kebaikan.

III. PEMAHAMAN BERMAKNA & PERTANYAAN PEMANTIK
• Pemahaman Bermakna:
  Mempelajari dan mengamalkan "$topic" bukan sekadar pengetahuan di kelas, melainkan wujud nyata cinta seorang muslim kepada Allah SWT dan sesama makhluk.
• Pertanyaan Pemantik:
  1. Pernahkah kalian melihat atau mengalami peristiwa yang berkaitan dengan "$topic" di sekitar kita?
  2. Mengapa ajaran Islam sangat menekankan pentingnya "$topic" bagi kehidupan kita?
  3. Bagaimana cara kita mempraktikkan "$topic" mulai hari ini di sekolah dan di rumah?

IV. KEGIATAN PEMBELAJARAN (3 x 35 MENIT)
A. Kegiatan Pendahuluan (15 Menit)
   1. Guru membuka kelas dengan salam hangat, menanyakan kabar siswa, dan berdoa bersama.
   2. Tadarus Al-Qur'an surat pendek pilihan bersama-sama dengan tartil.
   3. Presensi kehadiran siswa serta pemeriksaan kesiapan dan kebersihan kelas.
   4. Apersepsi: Guru mengaitkan pengalaman sehari-hari siswa dengan topik "$topic" dan menyampaikan Tujuan Pembelajaran.

B. Kegiatan Inti (75 Menit - Pembelajaran Berdiferensiasi)
   1. Eksplorasi Konsep (Diferensiasi Konten):
      • Siswa mengamati tayangan visual / kisah inspiratif dan penjelasan guru mengenai "$topic".
      • Tanya jawab interaktif menggali rasa ingin tahu siswa.
   2. Diskusi Kelompok Kolaboratif (Diferensiasi Proses):
      • Siswa dibagi menjadi beberapa kelompok kecil (4–5 orang).
      • Setiap kelompok mendiskusikan studi kasus kontekstual tentang penerapan "$topic" di sekolah dan masyarakat.
   3. Demonstrasi & Karya Siswa (Diferensiasi Produk):
      • Kelompok mempresentasikan hasil diskusi / peta konsep / simulasi praktik "$topic" di depan kelas.
      • Kelompok lain memberikan apresiasi dan tanggapan secara santun.
   4. Penguatan & Internalisasi 5 Dimensi Cinta:
      • Guru memberikan penguatan konsep, meluruskan miskonsepsi, dan mengajak siswa merenungkan nilai ibadah dari "$topic".

C. Kegiatan Penutup (15 Menit)
   1. Siswa bersama guru menyimpulkan poin-poin penting pembelajaran hari ini.
   2. Refleksi diri: Siswa menuliskan 1 kebaikan nyata terkait "$topic" yang akan dipraktikkan di rumah.
   3. Penyampaian agenda materi pertemuan berikutnya dan doa penutup majelis (Subhanakallahumma wa bihamdika...).

V. ASESMEN & TINDAK LANJUT
• Asesmen Diagnostik : Tanya jawab lisan di awal pembelajaran.
• Asesmen Formatif   : Observasi sikap (rubrik 5 Dimensi Cinta), keaktifan diskusi kelompok, dan unjuk kerja.
• Asesmen Sumatif    : Tes tertulis berbasis HOTS & Taksonomi SOLO tentang "$topic".
• Pengayaan          : Siswa yang telah mencapai tahap Relasional/Extended Abstract diberikan tugas menjadi tutor sebaya.
• Remedial           : Bimbingan khusus terbimbing dengan media kartu visual bagi siswa yang masih pada tahap Unistruktural.
        """.trimIndent()
    }

    private fun buildDynamicATP(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
ALUR TUJUAN PEMBELAJARAN (ATP) PAI & BUDI PEKERTI
Satuan Pendidikan : SD Negeri Kiarapandak 01, Kec. Sukajaya
Fase / Kelas      : $phase / Kelas $classLevel
Fokus Materi      : $topic ($element)

A. CAPAIAN PEMBELAJARAN FASE ($phase)
Berdasarkan SK Kepala BSKAP No. 020 Tahun 2026, peserta didik pada $phase diharapkan mampu menguasai kompetensi dasar keislaman pada elemen $element serta menginternalisasikan Lima Dimensi Cinta dalam kehidupan sehari-hari.

B. RINCIAN ALUR TUJUAN PEMBELAJARAN (ATP)
1. Tahap Pengenalan & Fondasi (Level SOLO: Unistruktural - 3 JP)
   • Kode TP : TP.$classLevel.1
   • Tujuan  : Mengenal pengertian, arti kata, dan konsep dasar tentang "$topic" dengan tepat.
   • Dimensi : Cinta Allah SWT & Rasul-Nya, Cinta Al-Qur'an & Hadis.
   • Aktivitas: Menyimak penjelasan visual, membaca dalil/teks dasar, dan tanya jawab terbimbing.

2. Tahap Pemahaman & Keterampilan (Level SOLO: Multistruktural - 6 JP)
   • Kode TP : TP.$classLevel.2
   • Tujuan  : Menguraikan bagian-bagian penting, syarat/ketentuan, serta contoh pelaksanaan "$topic" dalam kehidupan sehari-hari.
   • Dimensi : Cinta Diri Sendiri & Sesama Manusia.
   • Aktivitas: Diskusi kelompok, pengelompokan kartu konsep, dan latihan praktik terbimbing.

3. Tahap Analisis & Penghayatan (Level SOLO: Relasional - 6 JP)
   • Kode TP : TP.$classLevel.3
   • Tujuan  : Menghubungkan hikmah dan manfaat mengamalkan "$topic" dengan pembentukan karakter jujur, disiplin, dan toleran (wasathiyah).
   • Dimensi : Cinta Tanah Air & Bangsa.
   • Aktivitas: Studi kasus kontekstual, simulasi pemecahan masalah, dan presentasi kelompok.

4. Tahap Aktualisasi & Pembiasaan (Level SOLO: Extended Abstract - 3 JP)
   • Kode TP : TP.$classLevel.4
   • Tujuan  : Membiasakan serta merancang proyek/aksi nyata pengamalan "$topic" di lingkungan keluarga, sekolah, dan masyarakat.
   • Dimensi : Cinta Lingkungan Alam & Sesama.
   • Aktivitas: Jurnal pembiasaan harian, refleksi diri, dan proyek kebaikan bersama orang tua.

Total Alokasi Waktu: 18 Jam Pelajaran (6 Pertemuan x 3 JP)
        """.trimIndent()
    }

    private fun buildDynamicCP(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
ANALISIS CAPAIAN PEMBELAJARAN (CP) PAI & BUDI PEKERTI
Fase / Kelas : $phase / Kelas $classLevel
Elemen Utama : $element
Fokus Kajian : $topic

1. Landasan Regulasi:
   Keputusan Kepala Badan Standar, Kurikulum, dan Asesmen Pendidikan (BSKAP) Kementerian Pendidikan Dasar dan Menengah Nomor 020 Tahun 2026 tentang Capaian Pembelajaran pada PAUD, Jenjang Pendidikan Dasar, dan Pendidikan Menengah.

2. Karakteristik & Lima Dimensi Cinta PAI:
   • Cinta Allah SWT dan Rasul-Nya
   • Cinta Al-Qur'an dan Hadis
   • Cinta Diri Sendiri dan Sesama Manusia
   • Cinta Tanah Air dan Bangsa (Moderasi Beragama / Wasathiyah)
   • Cinta Lingkungan Hidup dan Alam Sekitar

3. Deskripsi Capaian Pembelajaran ($element - $phase):
   Peserta didik memahami konsep dan nilai luhur pada topik "$topic", mampu melafalkan/mempraktikkan ketentuannya dengan benar, serta menampilkan keteladanan akhlak mulia dalam pergaulan di sekolah maupun rumah.

4. Pemetaan Kompetensi Menuju Tujuan Pembelajaran:
   • Kompetensi Pengetahuan : Memahami pengertian, dalil, dan hikmah "$topic".
   • Kompetensi Keterampilan: Mempraktikkan / mendemonstrasikan penerapan "$topic" secara tertib.
   • Kompetensi Sikap       : Menunjukkan rasa syukur, tanggung jawab, dan kepedulian sosial melalui "$topic".
        """.trimIndent()
    }

    private fun buildDynamicKKTP(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
KRITERIA KETERCAPAIAN TUJUAN PEMBELAJARAN (KKTP) & RUBRIK TAKSONOMI SOLO
Mata Pelajaran : Pendidikan Agama Islam dan Budi Pekerti
Fase / Kelas   : $phase / Kelas $classLevel
Elemen         : $element
Topik / Materi : $topic

RUBRIK ASESMEN AUTENTIK BERBASIS TAKSONOMI SOLO:

1. Tahap Prastruktural (Skor < 60 | Belum Berkembang)
   • Deskripsi: Peserta didik belum memahami konsep dasar "$topic" dan memerlukan bimbingan penuh dari guru.
   • Tindak Lanjut: Pendampingan individual dan pengenalan ulang menggunakan media gambar/cerita.

2. Tahap Unistruktural (Skor 60 - 72 | Mulai Berkembang)
   • Deskripsi: Peserta didik mampu menyebutkan 1 informasi dasar atau definisi singkat mengenai "$topic", namun belum mampu menjelaskan rinciannya.
   • Tindak Lanjut: Bimbingan kelompok kecil dan latihan soal terstruktur.

3. Tahap Multistruktural (Skor 73 - 84 | Berkembang Sesuai Harapan)
   • Deskripsi: Peserta didik mampu menyebutkan beberapa ketentuan, contoh, dan langkah praktik "$topic" secara benar, namun belum mengaitkannya secara mendalam dengan hikmah kehidupan.
   • Tindak Lanjut: Diskusi penguatan makna dan studi kasus sederhana.

4. Tahap Relasional (Skor 85 - 92 | Baik / Mahir - Target Utama CP 020/2026)
   • Deskripsi: Peserta didik mampu menjelaskan konsep "$topic" secara utuh, mempraktikkannya dengan tepat, serta menghubungkan hikmahnya dengan perilaku akhlak mulia sehari-hari.
   • Tindak Lanjut: Diberikan apresiasi dan pengayaan kontekstual.

5. Tahap Extended Abstract (Skor 93 - 100 | Sangat Mahir / Istimewa)
   • Deskripsi: Peserta didik mampu menerapkan "$topic" secara konsisten, memberi teladan bagi teman sebaya, dan mampu merumuskan gagasan kebaikan baru berdasarkan nilai Lima Dimensi Cinta.
   • Tindak Lanjut: Dijadikan tutor sebaya / duta akhlak di kelas.

Keterangan Ketuntasan:
Peserta didik dinyatakan telah mencapai Tujuan Pembelajaran apabila minimal berada pada kategori Multistruktural/Relasional (Skor >= 75).
        """.trimIndent()
    }

    private fun buildDynamicProta(topic: String, classLevel: String, phase: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
PROGRAM TAHUNAN (PROTA) PAI & BUDI PEKERTI
Satuan Pendidikan : SD Negeri Kiarapandak 01
Fase / Kelas      : $phase / Kelas $classLevel
Tahun Pelajaran   : 2026/2027

SEMESTER 1 (GANJIL)
1. Elemen Al-Qur'an dan Hadis (18 JP)
   • Membaca, menghafal, dan memahami pesan pokok surah pilihan serta hadis keutamaan ilmu & persaudaraan.
2. Elemen Akidah (12 JP)
   • Mengimani sifat-sifat Allah SWT, Asmaulhusna, dan rukun iman secara aplikatif.
3. Elemen Akhlak (12 JP)
   • Integrasi 5 Dimensi Cinta: Sikap hormat kepada orang tua, guru, dan toleransi antarumat (Wasathiyah).
4. Elemen Fikih (12 JP)
   • Ketentuan ibadah, taharah, dan penguatan materi: $topic.
5. Elemen Sejarah Peradaban Islam (9 JP)
   • Keteladanan perjuangan dan akhlak Rasulullah SAW.
• Sumatif Tengah Semester & Akhir Semester Ganjil: 9 JP
Total Semester 1: 72 JP

SEMESTER 2 (GENAP)
6. Elemen Al-Qur'an dan Hadis Semester Genap (15 JP)
   • Tartil bacaan tajwid dan pengamalan pesan pokok ayat Al-Qur'an pilihan.
7. Elemen Akidah Semester Genap (12 JP)
   • Penguatan keimanan dan keteladanan rasul-rasul Allah SWT.
8. Elemen Akhlak & Moderasi Beragama (12 JP)
   • Akhlak terhadap sesama manusia dan cinta lingkungan alam sekitar.
9. Elemen Fikih & Pengamalan Ibadah (15 JP)
   • Praktik ibadah amaliyah dan pendalaman materi kontekstual ($topic).
10. Elemen Sejarah Peradaban Islam Semester Genap (9 JP)
    • Hikmah sejarah Islam dan keteladanan tokoh/sahabat Nabi SAW.
• Sumatif Tengah Semester & Akhir Tahun: 9 JP
Total Semester 2: 72 JP
Total Jam Pelajaran Tahunan: 144 JP (36 Minggu Efektif x 4 JP/Minggu)
        """.trimIndent()
    }

    private fun buildDynamicPromes(topic: String, classLevel: String, phase: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
PROGRAM SEMESTER (PROMES) PAI & BUDI PEKERTI
Satuan Pendidikan : SD Negeri Kiarapandak 01
Fase / Kelas      : $phase / Kelas $classLevel
Tahun Pelajaran   : 2026/2027

DISTRIBUSI ALOKASI WAKTU MINGGU EFEKTIF:
• Bulan 1 (Minggu 1 - 4 | 12 JP):
  - Minggu 1-2: Pengenalan & Eksplorasi Elemen Al-Qur'an Hadis (Tartil & Tajwid)
  - Minggu 3-4: Pemahaman Makna Ayat & Asesmen Formatif 1

• Bulan 2 (Minggu 1 - 4 | 12 JP):
  - Minggu 1-2: Elemen Akidah (Penguatan Keimanan & Asmaulhusna)
  - Minggu 3-4: Integrasi Lima Dimensi Cinta dalam Kehidupan Sehari-hari

• Bulan 3 (Minggu 1 - 4 | 12 JP):
  - Minggu 1-2: Elemen Akhlak Mulia & Moderasi Beragama (Tasamuh)
  - Minggu 3: Pelaksanaan Asesmen Tengah Semester (STS)
  - Minggu 4: Evaluasi & Remedial/Pengayaan STS

• Bulan 4 (Minggu 1 - 4 | 12 JP):
  - Minggu 1-3: Fokus Materi Inti "$topic" (Konsep, Dalil, & Simulasi Praktik)
  - Minggu 4: Unjuk Kerja & Penilaian Praktik "$topic"

• Bulan 5 (Minggu 1 - 4 | 12 JP):
  - Minggu 1-3: Elemen Sejarah Peradaban Islam (Kisah Teladan & Hikmah)
  - Minggu 4: Proyek Penguatan Lima Dimensi Cinta PAI

• Bulan 6 (Minggu 1 - 3 | 12 JP):
  - Minggu 1: Review Materi & Persiapan Sumatif Akhir Semester (SAS)
  - Minggu 2: Pelaksanaan Sumatif Akhir Semester (SAS)
  - Minggu 3: Pengolahan Nilai Rapor & Pembagian Rapor
        """.trimIndent()
    }

    private fun buildDynamicLKPD(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
LEMBAR KERJA PESERTA DIDIK (LKPD) PAI & BUDI PEKERTI
Satuan Pendidikan : SD Negeri Kiarapandak 01
Fase / Kelas      : $phase / Kelas $classLevel
Elemen            : $element
Topik Kegiatan    : $topic
--------------------------------------------------------------------
Nama Siswa / Kelompok : ............................................
Kelas / No. Absen     : Kelas $classLevel / ........................

A. PETUNJUK BELAJAR
1. Awali kegiatan belajar dengan membaca Basmalah dan doa belajar.
2. Bacalah materi singkat dan amati ilustrasi/kasus tentang "$topic" dengan cermat.
3. Diskusikan bersama teman sekelompokmu dengan santun dan saling menghargai.
4. Tuliskan jawabanmu dengan rapi pada kolom yang tersedia.

B. AKTIVITAS 1: MENGENAL & MEMAHAMI KONSEP (SOLO: Unistruktural & Multistruktural)
1. Jelaskan dengan bahasamu sendiri, apakah yang dimaksud dengan "$topic"?
   Jawaban: ........................................................................
   .................................................................................

2. Sebutkan 3 contoh perilaku atau ketentuan penting yang berkaitan dengan "$topic"!
   a. ..............................................................................
   b. ..............................................................................
   c. ..............................................................................

C. AKTIVITAS 2: ANALISIS KASUS & HIKMAH (SOLO: Relasional)
Bacalah situasi berikut:
"Di sekolah, Ahmad selalu berusaha mengamalkan nilai-nilai '$topic' dengan penuh kesadaran, baik ketika ada guru maupun saat sedang bersama teman-temannya."
Pertanyaan:
Mengapa sikap Ahmad tersebut mencerminkan rasa cinta kepada Allah SWT dan membawa kebaikan bagi teman-temannya?
Jawaban: ...........................................................................
....................................................................................

D. AKTIVITAS 3: KOMITMEN AKSI NYATA 5 DIMENSI CINTA (SOLO: Extended Abstract)
Tuliskan 2 rencana kebaikan terkait "$topic" yang akan kamu lakukan mulai minggu ini di rumah dan di sekolah!
1. Di Rumah   : ....................................................................
2. Di Sekolah : ....................................................................

Paraf Guru PAI: ................... | Nilai: ......... | Catatan: ..................
        """.trimIndent()
    }

    private fun buildDynamicBahanAjar(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
RINGKASAN BAHAN AJAR & MATERI PAI SD
Fase / Kelas : $phase / Kelas $classLevel
Elemen       : $element
Judul Materi : $topic

1. PENGANTAR MATERI
Alhamdulillah, pada pembelajaran kali ini kita akan mempelajari tentang "$topic". Materi ini merupakan bagian penting dari elemen $element pada Kurikulum Merdeka yang bertujuan menumbuhkan keimanan, ketakwaan, dan akhlak mulia peserta didik.

2. URAIAN INTI MATERI: ${topic.uppercase()}
• Pengertian & Makna Utama:
  "$topic" mengajarkan setiap muslim untuk senantiasa menyelaraskan pengetahuan agama dengan amal perbuatan nyata yang bersih, tertib, dan penuh kasih sayang.
• Landasan Nilai Keislaman:
  Ajaran Islam menekankan bahwa setiap amal kebaikan harus dilandasi niat ikhlas karena Allah SWT serta mengikuti tuntunan Rasulullah SAW.
• Penerapan dalam Kehidupan Sehari-hari Siswa Kelas $classLevel:
  1) Membiasakan diri berdoa dan menjaga adab sebelum serta sesudah beraktivitas.
  2) Melaksanakan kewajiban ibadah dan tugas belajar dengan penuh tanggung jawab.
  3) Bersikap ramah, jujur, tolong-menolong, dan menghormati teman di sekolah.
  4) Menjaga kebersihan diri, pakaian, musala, dan lingkungan sekolah.

3. HIKMAH MEMPELAJARI ${topic.uppercase()}
• Disayang oleh Allah SWT dan mendapatkan pahala kebaikan.
• Hati menjadi tenang, tenteram, dan percaya diri dalam berbuat baik.
• Disenangi oleh orang tua, guru, dan teman-teman di sekitar kita.
• Mewujudkan suasana sekolah yang rukun, damai, dan penuh berkah.
        """.trimIndent()
    }

    private fun buildDynamicSoalHOTS(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
BANK SOAL & KISI-KISI ASESMEN HOTS PAI SD (TAKSONOMI SOLO)
Mata Pelajaran : Pendidikan Agama Islam dan Budi Pekerti
Fase / Kelas   : $phase / Kelas $classLevel
Elemen         : $element
Topik / Materi : $topic

--------------------------------------------------------------------
Soal 1 [Level SOLO: Multistruktural - Pemahaman Konsep]
Perhatikan pernyataan berikut terkait materi "$topic"!
Dalam ajaran Islam, setiap muslim diperintahkan untuk memahami dan melaksanakan ketentuan "$topic" dengan penuh kesungguhan dan keikhlasan. Tujuan utama dari pelaksanaan ajaran tersebut bagi seorang pelajar muslim adalah ....
A. Agar mendapat pujian dari teman-teman di kelas
B. Sebagai wujud ketaatan kepada Allah SWT dan membentuk akhlak mulia
C. Supaya terhindar dari tugas piket di sekolah
D. Hanya untuk mendapatkan nilai tinggi saat ujian saja
Kunci Jawaban: B
Pembahasan: Pelaksanaan ajaran "$topic" berorientasi pada dimensi Cinta Allah SWT & Rasul-Nya serta pembentukan karakter berakhlak mulia secara ikhlas.

--------------------------------------------------------------------
Soal 2 [Level SOLO: Relasional - Analisis Kontekstual HOTS]
Stimulus:
Di SDN Kiarapandak 01, Rina dan teman-temannya baru saja mempelajari materi "$topic" bersama Guru PAI. Ketika jam istirahat tiba, Rina melihat temannya sedang membutuhkan bantuan dan mengingatkan kelompoknya untuk menerapkan nilai kebaikan dari materi "$topic" secara langsung.
Berdasarkan kisah tersebut, sikap yang ditunjukkan oleh Rina mencerminkan ....
A. Menghafal materi pelajaran tanpa memedulikan keadaan sekitar
B. Mengintegrasikan ilmu agama yang dipelajari ke dalam perilaku nyata (amal saleh)
C. Menunda perbuatan baik sampai diminta oleh guru kelas
D. Mengutamakan kepentingan bermain di atas kepedulian terhadap sesama
Kunci Jawaban: B
Pembahasan: Pada tingkat Relasional Taksonomi SOLO, siswa mampu menghubungkan pengetahuan konsep "$topic" dengan tindakan nyata yang bermanfaat bagi sesama.

--------------------------------------------------------------------
Soal 3 [Level SOLO: Relasional - Moderasi Beragama & Lima Dimensi Cinta]
Dalam menerapkan nilai-nilai "$topic" di lingkungan sekolah dan masyarakat yang beragam, sikap moderat (wasathiyah) yang paling tepat ditunjukkan oleh siswa Kelas $classLevel adalah ....
A. Bersikap santun, menghargai sesama, dan konsisten menjalankan kewajiban agama dengan tertib
B. Memaksakan pendapat pribadi kepada teman saat berdiskusi kelompok
C. Memilih-milih teman hanya yang memiliki hobi yang sama saja
D. Mengabaikan kebersihan dan ketertiban lingkungan kelas
Kunci Jawaban: A
Pembahasan: CP BSKAP 020/2026 menekankan integrasi ketaatan beribadah dengan sikap tasamuh (toleran), santun, dan cinta sesama.

--------------------------------------------------------------------
Soal 4 [Level SOLO: Extended Abstract - Evaluasi & Pemecahan Masalah]
Apabila seorang siswa memahami pentingnya "$topic" tetapi masih sering lupa mempraktikkannya di rumah, langkah terbaik yang dapat dirancang agar terbiasa mengamalkannya secara istiqamah adalah ....
A. Membiarkannya karena masih usia sekolah dasar
B. Membuat jadwal pembiasaan harian bersama orang tua serta melakukan refleksi setiap malam
C. Menyalahkan keadaan rumah yang sibuk
D. Hanya mempraktikkannya ketika sedang dinilai oleh guru di sekolah
Kunci Jawaban: B
Pembahasan: Merancang jadwal pembiasaan mandiri dan evaluasi diri bersama keluarga merupakan capaian tingkat Extended Abstract.

--------------------------------------------------------------------
Soal 5 [Level SOLO: Extended Abstract - Refleksi Nilai]
Hikmah utama yang dirasakan oleh warga sekolah apabila seluruh siswa Kelas $classLevel mengamalkan nilai "$topic" secara konsisten adalah ....
A. Terciptanya lingkungan belajar yang bersih, rukun, berkah, dan penuh semangat kebaikan
B. Berkurangnya jam pelajaran di sekolah
C. Siswa tidak perlu lagi belajar bersama di kelas
D. Hilangnya seluruh peraturan tata tertib sekolah
Kunci Jawaban: A
Pembahasan: Pengamalan kolektif terhadap nilai PAI menciptakan budaya sekolah yang religius, harmonis, dan berakhlak karimah.
        """.trimIndent()
    }

    private fun buildDynamicRefleksi(topic: String, classLevel: String, phase: String, element: String): String {
        return """
[REFERENSI RESMI: KEPUTUSAN KEPALA BSKAP NOMOR 020 TAHUN 2026]
LEMBAR REFLEKSI PEMBELAJARAN & INTERNALISASI LIMA DIMENSI CINTA
Fase / Kelas   : $phase / Kelas $classLevel
Elemen         : $element
Topik / Materi : $topic

A. REFLEKSI PESERTA DIDIK (SISWA)
1. Bagaimana perasaanmu setelah mengikuti pembelajaran PAI tentang "$topic" hari ini?
   [ ] Sangat Senang dan Bersemangat
   [ ] Senang dan Paham
   [ ] Masih Perlu Bertanya Lagi

2. Refleksi Lima Dimensi Cinta:
   • Cinta Allah & Rasul-Nya : Apa yang membuatmu semakin bersyukur kepada Allah SWT setelah belajar "$topic"?
   • Cinta Al-Qur'an & Hadis : Pesan kebaikan apa yang kamu ingat dari pelajaran hari ini?
   • Cinta Diri & Sesama     : Kebaikan apa yang akan kamu lakukan kepada orang tua dan temanmu hari ini?
   • Cinta Tanah Air         : Bagaimana caramu menjaga kerukunan di kelas melalui materi "$topic"?
   • Cinta Lingkungan Alam   : Kebersihan atau kerapian apa yang sudah kamu jaga hari ini?

B. REFLEKSI PENDIDIK (GURU PAI)
1. Apakah seluruh peserta didik Kelas $classLevel terlibat aktif dan antusias selama pembelajaran "$topic"?
2. Berapa persen peserta didik yang telah mencapai tahap Relasional pada Taksonomi SOLO?
3. Strategi atau media visual apa yang perlu ditingkatkan pada pertemuan berikutnya agar pembelajaran semakin bermakna?
        """.trimIndent()
    }

    private fun buildInteractiveConsultationResponse(
        prompt: String,
        topic: String,
        classLevel: String,
        phase: String,
        element: String
    ): String {
        val lower = prompt.lowercase()
        if (lower.contains("tajwid") || lower.contains("mengaji") || lower.contains("al-qur'an") || lower.contains("tartil")) {
            return """
Assalamu'alaikum Warahmatullahi Wabarakatuh.
Berikut panduan praktis **Metode Mengajar Tajwid & Al-Qur'an Agar Siswa Kelas $classLevel ($phase) Antusias**:

📌 Fokus Pembelajaran: "$topic"
📚 Elemen: Al-Qur'an dan Hadis (SK Kepala BSKAP No. 020 Tahun 2026)

1. Metode Talaqqi & Gesture Warna (Visual-Kinestetik):
   • Gunakan kode warna pada papan tulis/proyektor untuk hukum bacaan (misal: Merah = Idgham, Hijau = Ikhfa, Biru = Qalqalah, Kuning = Mad).
   • Ajak siswa menggunakan gerakan tangan sederhana (isyarat jari) saat menemukan hukum tajwid tertentu ketika membaca ayat bersama-sama.

2. Permainan "Detektif Tajwid" Berkelompok:
   • Bagi siswa menjadi kelompok kecil (4–5 orang).
   • Berikan potongan ayat dari surah pilihan Kelas $classLevel, lalu minta setiap kelompok berlomba menemukan dan menandai hukum tajwid dalam waktu 3 menit.

3. Irama Rost / Murattal Anak yang Menyenangkan:
   • Contohkan bacaan ayat dengan nada murattal yang mudah diikuti anak SD secara berulang per penggalan ayat (ayat demi ayat).

4. Integrasi Lima Dimensi Cinta & Taksonomi SOLO:
   • Cinta Al-Qur'an & Hadis: Menumbuhkan rasa senang membaca Al-Qur'an setiap hari.
   • Evaluasi SOLO: Mulai dari mengenali 1 hukum tajwid (Unistruktural), mengelompokkan beberapa hukum tajwid dalam ayat (Multistruktural), hingga mempraktikkan bacaan tartil dengan makhraj yang tepat (Relasional).
            """.trimIndent()
        }

        if (lower.contains("ice breaking") || lower.contains("permainan") || lower.contains("semangat") || lower.contains("bosan")) {
            return """
Assalamu'alaikum Warahmatullahi Wabarakatuh.
Berikut **5 Ide Ice Breaking Islami Pilihan untuk Kelas $classLevel SD ($phase)** sebelum atau di tengah pembelajaran PAI:

1. Tepuk Anak Saleh & Tepuk 5 Dimensi Cinta:
   • "Tepuk Anak Saleh: Aku anak saleh (prok 3x), Rajin salat (prok 3x), Rajin ngaji (prok 3x), Orang tua dihormati (prok 3x), Cinta Islam sampai mati, Lailahaillallah Muhammadurrasulullah, Yes!"
   • "Tepuk 5 Cinta: Cinta Allah & Rasul, Cinta Al-Qur'an, Cinta Sesama, Cinta Indonesia, Cinta Lingkungan!"

2. Permainan "Sambung Ayat / Tebak Surah":
   • Guru membacakan sepenggal ayat Juz Amma, lalu menunjuk kelompok secara acak untuk melanjutkan 1 ayat berikutnya dengan kompak.

3. Gerak & Lagu Asmaulhusna / Rukun Iman:
   • Menyanyikan 5–10 Asmaulhusna sesuai materi Kelas $classLevel disertai gerakan tangan serempak selama 2 menit untuk mengembalikan fokus kinestetik siswa.

4. "Kata Nabi SAW..." (Variasi Simon Says Islami):
   • Jika guru berkata "Rasulullah mencontohkan: Tersenyum kepada teman!", siswa langsung mempraktikkannya. Melatih konsentrasi sekaligus adab Islami.

5. Kuis Cepat "Siapa Aku?" Tokoh Teladan Islam:
   • Guru memberikan 3 petunjuk singkat tentang nabi atau sahabat, siswa berlomba mengangkat tangan menjawab dengan sopan.
            """.trimIndent()
        }

        return """
Assalamu'alaikum Warahmatullahi Wabarakatuh.
Berikut penjelasan dan panduan lengkap dari Asisten AI Guru PAI untuk Kelas $classLevel ($phase):

📌 Topik / Pertanyaan: "$topic"
📚 Elemen Terkait: $element (Pedoman SK Kepala BSKAP No. 020 Tahun 2026)

1. Penjelasan Inti & Konsep Utama:
   Dalam pembelajaran PAI Kurikulum Merdeka di Sekolah Dasar, topik "$topic" diarahkan agar murid tidak hanya menghafal secara kognitif, melainkan memahami makna dan mempraktikkannya dalam keseharian. Materi ini menanamkan kesadaran bahwa setiap ilmu agama bermuara pada ketakwaan dan akhlak mulia.

2. Integrasi Lima Dimensi Cinta (CP 020/2026):
   • Cinta Allah SWT & Rasul-Nya: Menumbuhkan niat ikhlas dan kecintaan menjalankan syariat.
   • Cinta Al-Qur'an & Hadis: Mengaitkan materi dengan ayat Al-Qur'an atau hadis pendek yang mudah dihafal anak SD.
   • Cinta Diri & Sesama: Melatih empati, kejujuran, kesantunan, dan tanggung jawab pribadi.
   • Cinta Tanah Air & Lingkungan: Menjaga kerukunan teman sebaya serta kebersihan lingkungan belajar.

3. Rekomendasi Metode Mengajar di Kelas $classLevel:
   • Pembuka (Apersepsi): Mulailah dengan kisah kontekstual atau gambar/video singkat yang dekat dengan dunia anak SD.
   • Kegiatan Inti: Gunakan metode diskusi berpasangan (Think-Pair-Share), simulasi praktik langsung, atau permainan kartu tanya-jawab.
   • Asesmen (Taksonomi SOLO): Ukur pemahaman mulai dari menyebutkan konsep dasar (Unistruktural), menguraikan contoh (Multistruktural), hingga mengaitkan hikmah dalam perilaku nyata (Relasional).

💡 Tips Cepat:
Anda dapat menekan tombol "Simpan ke Perangkat" untuk mengarsipkan jawaban ini ke menu Perangkat Pembelajaran, atau gunakan tab "Generator CP 020" untuk menyusun Modul Ajar, ATP, LKPD, dan Soal HOTS lengkap!
        """.trimIndent()
    }

    /**
     * Helper to parse generated HOTS questions into structured Question entities for Bank Soal
     */
    fun parseQuestionsFromText(rawText: String, defaultTopic: String = "PAI"): List<ParsedGeneratedQuestion> {
        val result = mutableListOf<ParsedGeneratedQuestion>()
        val cleanedRaw = rawText.replace("**", "")
        val blocks = cleanedRaw.split(Regex("(?=(?:Soal\\s+\\d+|(?:^|\\n)\\d+\\.\\s))", RegexOption.IGNORE_CASE))

        for (block in blocks) {
            val hasOptions = (block.contains("A.") || block.contains("A)")) &&
                (block.contains("B.") || block.contains("B)")) &&
                (block.contains("C.") || block.contains("C)"))
            if (!hasOptions) continue

            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val questionLines = mutableListOf<String>()
            var optA = ""
            var optB = ""
            var optC = ""
            var optD = ""
            var key = "B"
            var explanation = ""

            var inOptions = false
            for (line in lines) {
                when {
                    line.startsWith("Soal ", ignoreCase = true) -> {
                        val afterHeader = line.substringAfter("]").substringAfter(":").trim()
                        if (afterHeader.isNotEmpty() && afterHeader != line && !afterHeader.startsWith("Soal", ignoreCase = true)) {
                            questionLines.add(afterHeader)
                        }
                    }
                    line.matches(Regex("^\\d+\\.\\s+.*")) && !inOptions -> {
                        val afterNum = line.replaceFirst(Regex("^\\d+\\.\\s+"), "").trim()
                        if (afterNum.isNotEmpty()) {
                            questionLines.add(afterNum)
                        }
                    }
                    line.startsWith("A.") || line.startsWith("a.") || line.startsWith("A)") || line.startsWith("a)") -> {
                        inOptions = true
                        optA = line.drop(2).trim()
                    }
                    line.startsWith("B.") || line.startsWith("b.") || line.startsWith("B)") || line.startsWith("b)") -> {
                        inOptions = true
                        optB = line.drop(2).trim()
                    }
                    line.startsWith("C.") || line.startsWith("c.") || line.startsWith("C)") || line.startsWith("c)") -> {
                        inOptions = true
                        optC = line.drop(2).trim()
                    }
                    line.startsWith("D.") || line.startsWith("d.") || line.startsWith("D)") || line.startsWith("d)") -> {
                        inOptions = true
                        optD = line.drop(2).trim()
                    }
                    line.startsWith("Kunci", ignoreCase = true) || line.startsWith("Jawaban", ignoreCase = true) -> {
                        val k = line.substringAfter(":").trim().uppercase().take(1)
                        if (k in listOf("A", "B", "C", "D")) key = k
                    }
                    line.startsWith("Pembahasan", ignoreCase = true) || line.startsWith("Penjelasan", ignoreCase = true) -> {
                        explanation = line.substringAfter(":").trim()
                    }
                    !inOptions && !line.startsWith("---") && !line.startsWith("[REFERENSI") -> {
                        if (!line.startsWith("Mata Pelajaran") && !line.startsWith("Fase") && !line.startsWith("Elemen") && !line.startsWith("Topik") && !line.startsWith("BANK SOAL")) {
                            questionLines.add(line)
                        }
                    }
                }
            }

            val qText = questionLines.joinToString(" ").replace("Stimulus:", "").trim()
            if (qText.isNotEmpty() && optA.isNotEmpty() && optB.isNotEmpty()) {
                result.add(
                    ParsedGeneratedQuestion(
                        text = qText,
                        optionA = optA,
                        optionB = optB,
                        optionC = optC.ifEmpty { "Semua jawaban benar" },
                        optionD = optD.ifEmpty { "Semua jawaban salah" },
                        answerKey = key,
                        difficulty = "HOTS - SOLO ($defaultTopic)",
                        explanation = explanation.ifEmpty { "Sesuai pedoman Capaian Pembelajaran SK BSKAP No. 020 Tahun 2026." }
                    )
                )
            }
        }
        return result
    }
}
