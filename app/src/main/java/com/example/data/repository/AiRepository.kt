package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AiMessage
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

    // Lazy initialization of model as mandated by Firebase AI Logic skill
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
            },
            systemInstruction = content {
                text(
                    """
                    You are Hamza AI, the intelligent and friendly AI companion integrated into ChatMitra messaging app.
                    You are fluent in English, Hindi (हिन्दी), and Urdu (اردو).
                    Capabilities:
                    1. Help users draft, polish, and reply to chat messages with appropriate tone.
                    2. Correct English grammar and explain nuances gently.
                    3. Translate accurately between English, Hindi, and Urdu.
                    4. Summarize long conversations, notes, or articles.
                    5. Provide educational help and general knowledge answers clearly and concisely.
                    Style: Helpful, respectful, warm, and concise suitable for mobile chat.
                    """.trimIndent()
                )
            }
        )
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
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
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
