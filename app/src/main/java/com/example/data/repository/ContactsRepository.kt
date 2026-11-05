package com.example.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.R
import com.example.data.model.Contact
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class ContactsRepository(
    private val context: Context,
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        context = context,
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    /**
     * Fetches registered ChatMitra users from Firestore.
     */
    suspend fun getRegisteredChatMitraUsers(): Result<List<UserProfile>> {
        val path = "users"
        val currentUid = auth.currentUser?.uid ?: ""
        return try {
            val snapshot = db.collection(path).get().await()
            val list = snapshot.toObjects(UserProfile::class.java).filter { it.userId != currentUid }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Reads phone contacts safely using content resolver when permission is granted.
     */
    fun loadPhoneContacts(): Flow<List<Contact>> = flow {
        val contactsList = mutableListOf<Contact>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Contact" else "Contact"
                    val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""

                    if (number.isNotBlank()) {
                        contactsList.add(
                            Contact(
                                id = id,
                                name = name,
                                phoneNumber = number.replace(" ", "").replace("-", ""),
                                isChatMitraUser = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // SecurityException if not granted yet
        }
        emit(contactsList.distinctBy { it.phoneNumber })
    }.flowOn(Dispatchers.IO)

    /**
     * Searches for a registered user by unique ChatMitra handle.
     */
    suspend fun searchUserByChatMitraId(mitraId: String): Result<UserProfile?> {
        val path = "users"
        val clean = mitraId.trim().removePrefix("@").lowercase()
        return try {
            val snapshot = db.collection(path)
                .whereEqualTo("chatMitraId", clean)
                .get()
                .await()
            val user = snapshot.toObjects(UserProfile::class.java).firstOrNull()
            Result.success(user)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun addContact(name: String, phoneNumber: String, chatMitraId: String? = null): Result<Unit> {
        return Result.success(Unit)
    }
}
