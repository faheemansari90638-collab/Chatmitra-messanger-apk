package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AiMessage
import com.example.data.model.HamzaAiConfig
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "AiRepository"

class AiRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    var activeConfig: HamzaAiConfig = HamzaAiConfig()

    private fun buildSystemInstruction(config: HamzaAiConfig): String {
        val tonePrompt = when (config.tone) {
            "Professional & Formal" -> "Adopt a professional, executive, polished, courteous, and business-ready tone with crisp phrasing and respectful etiquette."
            "Concise & Direct" -> "Adopt an ultra-concise, direct, and to-the-point style. Avoid small talk, introductions, or pleasantries. Use bullet points for structured information."
            "Creative & Witty" -> "Adopt an expressive, creative, witty, and engaging tone with charming wordplay and clever analogies where appropriate."
            "Empathetic & Gentle" -> "Adopt an empathetic, thoughtful, calming, supportive, and deeply considerate tone. Validate feelings gently."
            else -> "Adopt a friendly, warm, supportive, approachable, and encouraging tone suitable for a trusted messaging companion."
        }

        val lengthPrompt = when (config.responseLength) {
            "Concise" -> "Keep your answer very short and concise (1-3 sentences or tight bullet points)."
            "Detailed" -> "Provide a comprehensive, detailed, step-by-step answer with thorough explanations."
            else -> "Keep your answer well-balanced, engaging, and easy to read on a mobile chat screen."
        }

        val emojiPrompt = when (config.emojiLevel) {
            "None" -> "Do NOT include any emojis in your response."
            "Expressive" -> "Use expressive, lively, and colorful emojis generously to make the message feel vibrant."
            else -> "Use emojis tastefully and naturally (1-2 per message where helpful)."
        }

        val customPrompt = if (config.customInstructions.isNotBlank()) {
            "\nUser Persona Rules:\n${config.customInstructions.trim()}"
        } else ""

        val grammarPrompt = if (config.grammarExplanation) {
            "When correcting grammar or editing text, provide a brief, helpful explanation of the rule or nuance."
        } else {
            "When correcting grammar or editing text, provide only the perfected output directly without lengthy rule explanations."
        }

        return """
            You are Hamza AI, the intelligent and personalized messaging companion integrated into ChatMitra messaging app.
            You are fluent in English, Hindi (हिन्दी), and Urdu (اردو).
            
            ACTIVE USER TONE & STYLE PREFERENCES:
            - Tone: $tonePrompt
            - Length: $lengthPrompt
            - Emoji Usage: $emojiPrompt
            - Grammar Handling: $grammarPrompt
            $customPrompt
            
            CORE CAPABILITIES:
            1. Help users draft, refine, polish, and reply to messages according to their active tone.
            2. Correct grammar and translate seamlessly between English, Hindi, and Urdu.
            3. Summarize long conversations, notes, or articles.
            4. Provide educational help and general knowledge answers clearly and concisely.
            
            Formatting: Clean mobile Markdown, readable paragraphs, and bullet points where helpful.
        """.trimIndent()
    }

    private fun getTemperatureForTone(tone: String): Float = when (tone) {
        "Professional & Formal" -> 0.35f
        "Concise & Direct" -> 0.25f
        "Creative & Witty" -> 0.85f
        "Empathetic & Gentle" -> 0.65f
        else -> 0.7f // Friendly & Warm
    }

    fun getGenerativeModel(config: HamzaAiConfig = activeConfig) = Firebase.ai.generativeModel(
        modelName = "gemini-3.8-flash",
        generationConfig = generationConfig {
            temperature = getTemperatureForTone(config.tone)
            topK = 40
        },
        systemInstruction = content {
            text(buildSystemInstruction(config))
        }
    )

    private val defaultModel by lazy {
        getGenerativeModel(activeConfig)
    }

    private val model get() = getGenerativeModel(activeConfig)

    suspend fun testToneReply(testPrompt: String, config: HamzaAiConfig = activeConfig): Result<String> {
        return try {
            val dynamicModel = getGenerativeModel(config)
            val response = dynamicModel.generateContent(testPrompt)
            Result.success(response.text?.trim() ?: "No response generated.")
        } catch (e: Exception) {
            Log.e(TAG, "Test tone generation failed", e)
            Result.failure(e)
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to use Hamza AI.")
    }

    fun observeAiMessages(): Flow<List<AiMessage>> = callbackFlow {
        val uid = requireUserId()
        val path = "users/$uid/ai_messages"

        val query = db.collection("users")
            .document(uid)
            .collection("ai_messages")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(AiMessage::class.java)
                    .sortedBy { it.createdAt?.seconds ?: 0L }
                trySend(list)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Sends prompt to Hamza AI with streaming response and saves to Firestore.
     */
    suspend fun sendPrompt(
        prompt: String,
        language: String = "en",
        config: HamzaAiConfig? = null,
        onChunkReceived: (String) -> Unit
    ): Result<String> {
        val uid = requireUserId()
        val userMsgId = "ai_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val userPath = "users/$uid/ai_messages/$userMsgId"

        // 1. Save user prompt
        try {
            db.collection("users").document(uid).collection("ai_messages").document(userMsgId).set(
                mapOf(
                    "id" to userMsgId,
                    "userId" to uid,
                    "role" to "user",
                    "content" to prompt.trim(),
                    "language" to language,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userPath)
            return Result.failure(e)
        }

        // 2. Stream generation from Gemini
        val accumulated = StringBuilder()
        return try {
            val targetModel = getGenerativeModel(config ?: activeConfig)
            withTimeout(60_000L) {
                targetModel.generateContentStream(prompt).collect { chunk ->
                    val text = chunk.text.orEmpty()
                    accumulated.append(text)
                    onChunkReceived(accumulated.toString())
                }
            }

            val finalReply = accumulated.toString().trim()
            if (finalReply.isNotEmpty()) {
                val modelMsgId = "ai_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val modelPath = "users/$uid/ai_messages/$modelMsgId"
                db.collection("users").document(uid).collection("ai_messages").document(modelMsgId).set(
                    mapOf(
                        "id" to modelMsgId,
                        "userId" to uid,
                        "role" to "model",
                        "content" to finalReply,
                        "language" to language,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(finalReply)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Hamza AI generation timed out", e)
            Result.failure(Exception("Hamza AI took too long to respond. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Hamza AI generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun summarizeConversation(messages: List<Pair<String, String>>): Result<String> {
        if (messages.isEmpty()) return Result.success("No messages available to summarize.")
        val conversationText = messages.joinToString("\n") { "${it.first}: ${it.second}" }
        val prompt = """
            Summarize this chat conversation cleanly for the user.
            Highlight the key discussion points, context, and outcomes.
            Keep it structured with bullet points.
            
            Conversation:
            $conversationText
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            Result.success(response.text?.trim() ?: "Summary could not be generated.")
        } catch (e: Exception) {
            Log.e(TAG, "Summarization failed", e)
            Result.failure(e)
        }
    }

    suspend fun extractTasksAndDates(messages: List<Pair<String, String>>): Result<String> {
        if (messages.isEmpty()) return Result.success("No messages available.")
        val conversationText = messages.joinToString("\n") { "${it.first}: ${it.second}" }
        val prompt = """
            Analyze the following conversation and extract:
            1. Action Items / Tasks (who agreed to do what)
            2. Dates, Deadlines, and Times mentioned
            3. Important Decisions made
            
            If none are found, say "No explicit tasks or dates found in this conversation."
            
            Conversation:
            $conversationText
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            Result.success(response.text?.trim() ?: "No tasks found.")
        } catch (e: Exception) {
            Log.e(TAG, "Task extraction failed", e)
            Result.failure(e)
        }
    }

    suspend fun askAboutChat(messages: List<Pair<String, String>>, question: String): Result<String> {
        val conversationText = messages.joinToString("\n") { "${it.first}: ${it.second}" }
        val prompt = """
            Answer the user's question specifically based on the provided conversation context.
            If the answer cannot be found in the conversation, state that politely.
            
            Question: $question
            
            Conversation Context:
            $conversationText
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            Result.success(response.text?.trim() ?: "Could not find an answer in this conversation.")
        } catch (e: Exception) {
            Log.e(TAG, "Ask chat failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateMessage(text: String, targetLanguage: String): Result<String> {
        val prompt = """
            Translate the following message into $targetLanguage accurately and naturally while preserving conversational tone and emojis.
            Return ONLY the translated text without extra explanation.
            
            Message:
            $text
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            Result.success(response.text?.trim() ?: text)
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun transcribeAudio(voiceLabel: String, speechLanguage: String = "English"): Result<String> {
        val prompt = """
            Transcribe and interpret this voice message recorded in ChatMitra ($speechLanguage context).
            Simulated audio description: $voiceLabel.
            Generate a realistic, helpful transcription of the audio content in $speechLanguage.
            Return ONLY the transcribed text.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            Result.success(response.text?.trim() ?: "Voice transcription unavailable.")
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(e)
        }
    }

    suspend fun clearAiHistory(): Result<Unit> {
        val uid = requireUserId()
        return try {
            val snapshots = db.collection("users").document(uid).collection("ai_messages").get().await()
            val batch = db.batch()
            for (doc in snapshots.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/ai_messages")
            Result.failure(e)
        }
    }
}
