package com.learnmate.ai.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["profileId", "updatedAt"])]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
