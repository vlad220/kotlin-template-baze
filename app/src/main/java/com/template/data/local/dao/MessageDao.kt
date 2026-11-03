package com.template.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.template.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query(
        "SELECT * FROM chat_messages " +
            "WHERE (senderId = :currentUserId AND recipientId = :peerId) " +
            "OR (senderId = :peerId AND recipientId = :currentUserId) " +
            "ORDER BY timestamp ASC"
    )
    fun getMessages(currentUserId: String, peerId: String): Flow<List<ChatMessageEntity>>
}
