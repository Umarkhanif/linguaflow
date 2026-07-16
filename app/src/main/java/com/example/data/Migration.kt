package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_progress (
                userId TEXT NOT NULL,
                streakCount INTEGER NOT NULL DEFAULT 0,
                lastActiveDate TEXT NOT NULL DEFAULT '',
                xpTotal INTEGER NOT NULL DEFAULT 0,
                dailyGoalXp INTEGER NOT NULL DEFAULT 50,
                wordsLearned INTEGER NOT NULL DEFAULT 0,
                lessonsCompleted INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(userId)
            )
            """.trimIndent()
        )
    }
}
