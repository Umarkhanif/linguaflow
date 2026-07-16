package com.example.data.repository

import com.example.data.SpeechHistory
import com.example.data.SpeechHistoryDao
import kotlinx.coroutines.flow.Flow

class SpeechRepository(private val dao: SpeechHistoryDao) {
    fun getAllHistory(): Flow<List<SpeechHistory>> = dao.getAllHistory()
    suspend fun insertHistory(history: SpeechHistory) = dao.insertHistory(history)
    suspend fun deleteHistory(id: Int) = dao.deleteHistory(id)
    suspend fun deleteAllHistory() = dao.deleteAllHistory()
}
