package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isLocked: Boolean = false,
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val passwordHint: String = "",
    val category: String = "شخصی",
    val recipientEmail: String = "",
    val colorHex: String = "#FFFFFF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
