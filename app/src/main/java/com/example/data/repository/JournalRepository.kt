package com.example.data.repository

import com.example.data.database.JournalDao
import com.example.data.database.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    suspend fun insertEntry(entry: JournalEntry) {
        journalDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: JournalEntry) {
        journalDao.deleteEntry(entry)
    }

    suspend fun deleteEntryById(id: Int) {
        journalDao.deleteEntryById(id)
    }
}
