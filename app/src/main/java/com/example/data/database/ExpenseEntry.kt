package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = ""
)
