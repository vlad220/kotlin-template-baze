package com.template.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val recipientId: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val status: String = "sent"
)
