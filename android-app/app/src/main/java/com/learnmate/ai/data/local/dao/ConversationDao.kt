package com.learnmate.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnmate.ai.data.local.entities.ChatMessageEntity
import com.learnmate.ai.data.local.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun getConversationsForProfileFlow(profileId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE profileId = :profileId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestConversationForProfile(profileId: String): ConversationEntity?

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY orderIndex ASC, id ASC")
    fun getMessagesForConversationFlow(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY orderIndex ASC, id ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationMeta(conversationId: String, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTimestamp(conversationId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET text = :text, runtimeSource = :runtimeSource WHERE id = :messageId")
    suspend fun updateMessage(messageId: Long, text: String, runtimeSource: String)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversations WHERE profileId = :profileId")
    suspend fun deleteConversationsForProfile(profileId: String)
}
