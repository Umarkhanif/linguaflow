package com.example.data.repository

import com.example.data.Word
import com.example.data.WordDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WordRepositoryTest {

    private lateinit var wordDao: WordDao
    private lateinit var repository: WordRepository

    @Before
    fun setup() {
        wordDao = TestWordDao()
        repository = WordRepository(wordDao)
    }

    @After
    fun teardown() {
        // cleanup if needed
    }

    @Test
    fun `getAllWords returns empty list when no data`() = runTest {
        val words = repository.getAllWords().first()
        assertEquals(emptyList<Word>(), words)
    }

    @Test
    fun `getAllWords returns inserted word`() = runTest {
        val word = Word(
            id = 1,
            kanji = "日本",
            reading = "にほん",
            translationIndonesian = "Jepang",
            translationEnglish = "Japan",
            category = "Kata Benda",
            jlptLevel = "N5",
            examplesJson = "",
            formsJson = ""
        )
        wordDao.insertAll(listOf(word))

        val words = repository.getAllWords().first()
        assertEquals(1, words.size)
        assertEquals("日本", words[0].kanji)
    }

    @Test
    fun `updateBookmark calls dao update`() = runTest {
        val word = Word(
            id = 1,
            kanji = "食べる",
            reading = "たべる",
            translationIndonesian = "makan",
            translationEnglish = "To eat",
            category = "Kata Kerja",
            jlptLevel = "N5",
            examplesJson = "",
            formsJson = ""
        )
        wordDao.insertAll(listOf(word))

        repository.updateBookmark(1, true)
        
        val bookmarked = wordDao.getBookmarkedWords().first()
        assertEquals(1, bookmarked.size)
        assertEquals(true, bookmarked[0].bookmarked)
    }

    @Test
    fun `getWordsByLevel filters by level`() = runTest {
        val n5Word = Word(
            id = 1,
            kanji = "食べる",
            reading = "たべる",
            translationIndonesian = "makan",
            translationEnglish = "To eat",
            category = "Kata Kerja",
            jlptLevel = "N5",
            examplesJson = "",
            formsJson = ""
        )
        wordDao.insertAll(listOf(n5Word))

        val words = repository.getWordsByLevel("N5").first()
        assertEquals(1, words.size)
        assertEquals("N5", words[0].jlptLevel)
    }

    @Test
    fun `getWordCount returns correct count`() = runTest {
        assertEquals(0, wordDao.getWordCount())
        
        val word1 = Word(
            id = 1, kanji = "一", reading = "いち", 
            translationIndonesian = "satu", translationEnglish = "one",
            category = "Kata Angka", jlptLevel = "N5", examplesJson = "", formsJson = ""
        )
        val word2 = Word(
            id = 2, kanji = "二", reading = "に",
            translationIndonesian = "dua", translationEnglish = "two",
            category = "Kata Angka", jlptLevel = "N5", examplesJson = "", formsJson = ""
        )
        wordDao.insertAll(listOf(word1, word2))
        
        assertEquals(2, wordDao.getWordCount())
    }
}

class TestWordDao : WordDao {
    private val words = mutableListOf<Word>()
    private val wordsFlow = MutableStateFlow<List<Word>>(emptyList())
    private val bookmarkedFlow = MutableStateFlow<List<Word>>(emptyList())
    private var wordCount = 0

    override fun getAllWordsFlow(): kotlinx.coroutines.flow.Flow<List<Word>> = wordsFlow
    
    override fun getWordsByLevel(level: String): kotlinx.coroutines.flow.Flow<List<Word>> = flow {
        emit(words.filter { it.jlptLevel == level })
    }
    
    override fun getBookmarkedWords(): kotlinx.coroutines.flow.Flow<List<Word>> = bookmarkedFlow
    
    override suspend fun insertAll(words: List<Word>) {
        this.words.addAll(words)
        wordCount += words.size
        wordsFlow.value = words.toList()
        bookmarkedFlow.value = words.filter { it.bookmarked }.toList()
    }
    
    override suspend fun updateWord(word: Word) {
        val index = words.indexOfFirst { it.id == word.id }
        if (index >= 0) {
            words[index] = word
            wordsFlow.value = words.toList()
            bookmarkedFlow.value = words.filter { it.bookmarked }.toList()
        }
    }
    
    override suspend fun updateBookmark(id: Int, bookmarked: Boolean) {
        val word = words.find { it.id == id }
        if (word != null) {
            val updated = word.copy(bookmarked = bookmarked)
            val index = words.indexOfFirst { it.id == id }
            if (index >= 0) {
                words[index] = updated
                wordsFlow.value = words.toList()
                bookmarkedFlow.value = words.filter { it.bookmarked }.toList()
            }
        }
    }
    
    override suspend fun updateLearned(id: Int, learned: Boolean) {
        val word = words.find { it.id == id }
        if (word != null) {
            val updated = word.copy(learned = learned)
            val index = words.indexOfFirst { it.id == id }
            if (index >= 0) {
                words[index] = updated
                wordsFlow.value = words.toList()
                bookmarkedFlow.value = words.filter { it.bookmarked }.toList()
            }
        }
    }
    
    override suspend fun getWordById(id: Int): Word? = words.find { it.id == id }
    
    override suspend fun getWordCount(): Int = wordCount
}