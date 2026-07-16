package com.example.data.repository

import com.example.data.UserProgress
import com.example.data.UserProgressDao
import kotlinx.coroutines.flow.Flow

class UserProgressRepository(private val dao: UserProgressDao) {
    fun getProgress(id: String = "local"): Flow<UserProgress?> = dao.getProgress(id)
    suspend fun upsert(progress: UserProgress) = dao.upsert(progress)
    suspend fun updateStreak(id: String, streak: Int, date: String) = dao.updateStreak(id, streak, date)
    suspend fun updateXp(id: String, xp: Int) = dao.updateXp(id, xp)
    suspend fun updateWordsLearned(id: String, count: Int) = dao.updateWordsLearned(id, count)
    suspend fun updateLessonsCompleted(id: String, count: Int) = dao.updateLessonsCompleted(id, count)
}
