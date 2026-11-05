package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_conversations")
data class CachedConversationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val participantsCsv: String,
    val groupTitle: String?,
    val groupDescription: String?,
    val lastMessageText: String?,
    val lastMessageSenderId: String?,
    val lastMessageTimestampMs: Long,
    val isMuted: Boolean,
    val isArchived: Boolean,
    val unreadCount: Int
)

@Entity(tableName = "cached_messages")
data class CachedMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String,
    val mediaUrl: String?,
    val mediaName: String?,
    val mediaSize: String?,
    val replyToMessageId: String?,
    val replyToText: String?,
    val status: String,
    val isPinned: Boolean,
    val isStarred: Boolean,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val timestampMs: Long
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM cached_conversations ORDER BY lastMessageTimestampMs DESC")
    fun observeConversations(): Flow<List<CachedConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<CachedConversationEntity>)

    @Query("SELECT * FROM cached_messages WHERE conversationId = :convId ORDER BY timestampMs ASC")
    fun observeMessages(convId: String): Flow<List<CachedMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessageEntity>)

    @Query("DELETE FROM cached_messages WHERE conversationId = :convId")
    suspend fun clearMessagesForConversation(convId: String)

    @Query("DELETE FROM cached_conversations")
    suspend fun clearAllConversations()

    @Query("DELETE FROM cached_messages")
    suspend fun clearAllMessages()
}

@Database(entities = [CachedConversationEntity::class, CachedMessageEntity::class], version = 1, exportSchema = false)
abstract class ChatMitraDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
