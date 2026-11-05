package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthRepository"

class AuthRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        context = context,
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private val credentialManager = CredentialManager.create(context)
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private var profileListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            val user = firebaseAuth.currentUser
            if (user != null) {
                listenToUserProfile(user.uid)
            } else {
                profileListener?.remove()
                _userProfile.value = null
            }
        }
    }

    private fun listenToUserProfile(uid: String) {
        profileListener?.remove()
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "users/$uid")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _userProfile.value = snapshot.toObject(UserProfile::class.java)
                } else {
                    _userProfile.value = null
                }
            }
    }

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing ChatMitra.")
    }

    suspend fun attemptAutoSignIn(): Boolean {
        if (auth.currentUser != null) {
            return true
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "default_web_client_id missing", e)
            return false
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(authCredential).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent auto-sign-in skipped: ${e.message}")
            false
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                } else {
                    onError("Unexpected credential format received.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    suspend fun saveUserProfile(
        displayName: String,
        phoneNumber: String,
        chatMitraId: String,
        about: String,
        avatarUrl: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        return try {
            val snapshot = userDoc.get().await()
            val cleanMitraId = chatMitraId.trim().removePrefix("@").lowercase()
            if (snapshot.exists()) {
                val updateData = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "chatMitraId" to cleanMitraId,
                    "about" to about.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (avatarUrl != null) {
                    updateData["avatarUrl"] = avatarUrl
                }
                userDoc.update(updateData).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "chatMitraId" to cleanMitraId,
                    "about" to about.trim(),
                    "isOnline" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (avatarUrl != null) {
                    payload["avatarUrl"] = avatarUrl
                }
                userDoc.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = db.collection("users").document(uid)
        try {
            userDoc.update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update presence: ${e.message}")
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                updatePresence(false)
                auth.signOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Sign out error", e)
            } finally {
                onComplete()
            }
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val uid = requireUserId()
        return try {
            db.collection("users").document(uid).delete().await()
            auth.currentUser?.delete()?.await()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
