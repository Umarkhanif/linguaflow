package com.example.data.repository

import com.example.data.PracticeHistory
import com.example.data.PracticeHistoryDao
import kotlinx.coroutines.flow.Flow

class PracticeRepository(private val dao: PracticeHistoryDao) {
    fun getAllHistory(): Flow<List<PracticeHistory>> = dao.getAllHistoryFlow()
    suspend fun insertHistory(history: PracticeHistory) = dao.insertHistory(history)
    suspend fun clearHistory() = dao.clearHistory()
}
