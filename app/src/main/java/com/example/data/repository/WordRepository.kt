package com.example.data.repository

import com.example.data.Word
import com.example.data.WordDao
import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    fun getAllWords(): Flow<List<Word>> = wordDao.getAllWordsFlow()
    fun getBookmarkedWords(): Flow<List<Word>> = wordDao.getBookmarkedWords()
    fun getWordsByLevel(level: String): Flow<List<Word>> = wordDao.getWordsByLevel(level)
    suspend fun getWordById(id: Int): Word? = wordDao.getWordById(id)
    suspend fun updateBookmark(id: Int, bookmarked: Boolean) = wordDao.updateBookmark(id, bookmarked)
    suspend fun updateLearned(id: Int, learned: Boolean) = wordDao.updateLearned(id, learned)
    suspend fun insertAll(words: List<Word>) = wordDao.insertAll(words)
}
