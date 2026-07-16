package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :id")
    fun getProgress(id: String): Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgress)

    @Query("UPDATE user_progress SET streakCount = :streak, lastActiveDate = :date WHERE userId = :id")
    suspend fun updateStreak(id: String, streak: Int, date: String)

    @Query("UPDATE user_progress SET xpTotal = :xp WHERE userId = :id")
    suspend fun updateXp(id: String, xp: Int)

    @Query("UPDATE user_progress SET wordsLearned = :count WHERE userId = :id")
    suspend fun updateWordsLearned(id: String, count: Int)

    @Query("UPDATE user_progress SET lessonsCompleted = :count WHERE userId = :id")
    suspend fun updateLessonsCompleted(id: String, count: Int)
}
