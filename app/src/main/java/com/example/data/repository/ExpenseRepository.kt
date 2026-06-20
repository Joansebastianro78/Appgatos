package com.example.data.repository

import com.example.data.database.ExpenseDao
import com.example.data.database.ExpenseEntry
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<ExpenseEntry>> = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: ExpenseEntry) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntry) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }
}
