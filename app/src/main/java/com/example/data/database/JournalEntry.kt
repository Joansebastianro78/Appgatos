package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val companionType: String,
    val aiResponse: String,
    val moodVibe: String,
    val timestamp: Long = System.currentTimeMillis()
)
