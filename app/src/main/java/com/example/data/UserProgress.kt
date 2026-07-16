package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val userId: String = "local",
    val streakCount: Int = 0,
    val lastActiveDate: String = "", // yyyy-MM-dd
    val xpTotal: Int = 0,
    val dailyGoalXp: Int = 50,
    val wordsLearned: Int = 0,
    val lessonsCompleted: Int = 0
)
