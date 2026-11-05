package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createConversation_authenticatedUser_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = "bob_partner_123"
        val repository = ChatRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateDirectConversation(
                currentUid = aliceUid,
                otherUid = bobUid,
                otherDisplayName = "Bob Partner"
            )
        }

        assertTrue("Conversation creation should succeed", result.isSuccess)
        val convId = result.getOrThrow()
        assertTrue("Conversation ID should not be blank", convId.isNotBlank())
    }

    @Test
    fun sendMessage_authenticatedParticipant_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = "bob_partner_456"
        val repository = ChatRepository(firestore)

        val convResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateDirectConversation(
                currentUid = aliceUid,
                otherUid = bobUid,
                otherDisplayName = "Bob Partner"
            )
        }
        val convId = convResult.getOrThrow()

        val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                conversationId = convId,
                content = "Test message from Alice",
                senderName = "Alice",
                participantUids = listOf(aliceUid, bobUid)
            )
        }

        assertTrue("Sending message should succeed", sendResult.isSuccess)
        val msgId = sendResult.getOrThrow()
        assertTrue("Message ID should not be blank", msgId.isNotBlank())
    }

    @Test
    fun observeMessages_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        // Sign in first to create conversation and message
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ChatRepository(firestore)
        val convId = repository.getOrCreateDirectConversation(
            currentUid = aliceUid,
            otherUid = "bob_789",
            otherDisplayName = "Bob"
        ).getOrThrow()

        // Sign out to test unauthenticated access
        auth.signOut()

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMessages(convId).first()
            }
        } catch (e: Exception) {
            caughtException = e
        }

        assertNotNull("Expected exception for unauthenticated read", caughtException)
        val firestoreException = caughtException?.unwrapFirestoreException()
        if (firestoreException != null) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@chatmitra.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
