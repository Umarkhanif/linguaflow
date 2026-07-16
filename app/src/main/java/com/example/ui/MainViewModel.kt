package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.EvaluationResult
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.PracticeHistory
import com.example.data.SpeechHistory
import com.example.data.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    MAIN,
    FLASHCARD,
    QUIZ
}

enum class MainTab {
    BELAJAR,
    LATIHAN,
    KAMUS,
    PERINGKAT,
    UCAPAN,
    PROFIL
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class SpeechExercise(
    val id: Int,
    val text: String,
    val romaji: String,
    val indexText: String = "3/10"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val wordDao = db.wordDao()
    private val practiceHistoryDao = db.practiceHistoryDao()
    private val speechHistoryDao = db.speechHistoryDao()

    // ─── DB flows ───────────────────────────────────────────────────────────
    val allWords: StateFlow<List<Word>> = wordDao.getAllWordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val histories: StateFlow<List<PracticeHistory>> = practiceHistoryDao.getAllHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechHistory: StateFlow<List<SpeechHistory>> = speechHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── App Navigation States ───────────────────────────────────────────────
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen = _currentScreen.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.BELAJAR)
    val activeTab = _activeTab.asStateFlow()

    // ─── App Theme Preference ────────────────────────────────────────────────
    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme = _appTheme.asStateFlow()

    // Theme mode string (for compatibility with speech screens)
    private val _themeMode = MutableStateFlow("Sistem")
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        _appTheme.value = when (mode) {
            "Terang" -> AppTheme.LIGHT
            "Gelap" -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    // ─── Onboarding info ─────────────────────────────────────────────────────
    private val _userName = MutableStateFlow("Tamu")
    val userName = _userName.asStateFlow()

    // ─── Word Detail dialog ───────────────────────────────────────────────────
    private val _selectedWordForDetail = MutableStateFlow<Word?>(null)
    val selectedWordForDetail = _selectedWordForDetail.asStateFlow()

    // ─── Flashcard Session States ─────────────────────────────────────────────
    private val _activeFlashcards = MutableStateFlow<List<Word>>(emptyList())
    val activeFlashcards = _activeFlashcards.asStateFlow()

    private val _currentFlashcardIndex = MutableStateFlow(0)
    val currentFlashcardIndex = _currentFlashcardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped = _isCardFlipped.asStateFlow()

    // ─── Interactive Quiz Session States ─────────────────────────────────────
    private val _quizSelectedWordChips = MutableStateFlow<List<String>>(emptyList())
    val quizSelectedWordChips = _quizSelectedWordChips.asStateFlow()

    private val _quizCorrectState = MutableStateFlow<Boolean?>(null)
    val quizCorrectState = _quizCorrectState.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex = _currentQuizIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore = _quizScore.asStateFlow()

    private val _quizAudioPlaying = MutableStateFlow(false)
    val quizAudioPlaying = _quizAudioPlaying.asStateFlow()

    // ─── AI Sensei Chat States ────────────────────────────────────────────────
    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Halo! Saya adalah AI Sensei. Tanyakan apa saja tentang tata bahasa Jepang, perbedaan partikel, atau cara penggunaan kalimat. Saya di sini untuk membantu belajarmu dengan berpikir mendalam! 🧠✨", isUser = false)
        )
    )
    val aiChatMessages = _aiChatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking = _isAiThinking.asStateFlow()

    private val _aiSenseiExpanded = MutableStateFlow(false)
    val aiSenseiExpanded = _aiSenseiExpanded.asStateFlow()

    // ─── Mock Leaderboard Data ────────────────────────────────────────────────
    val leaderboard = listOf(
        Pair("Wildan", 1450),
        Pair("Asep", 1280),
        Pair("Siti", 1120),
        Pair("Tamu (Kamu)", 850),
        Pair("Budi", 710),
        Pair("Dewi", 640),
        Pair("Eko", 520)
    )

    // ─── Speech Practice States (from linguaflow2) ───────────────────────────
    val exercises = listOf(
        SpeechExercise(1, "私は毎日日本語を勉強します。", "Watashi wa mainichi nihongo o benkyou shimasu.", "3/10"),
        SpeechExercise(2, "こんにちは、お元気ですか？", "Konnichiwa, o-genki desu ka?", "4/10"),
        SpeechExercise(3, "美味しい寿司を食べたいです。", "Oishii sushi o tabetai desu.", "5/10"),
        SpeechExercise(4, "銀行はどこにありますか？", "Ginkou wa doko ni arimasu ka?", "6/10")
    )

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex = _currentExerciseIndex.asStateFlow()

    val currentExercise: StateFlow<SpeechExercise> = MutableStateFlow(exercises[0]).apply {
        viewModelScope.launch {
            _currentExerciseIndex.collect { index ->
                value = exercises[index % exercises.size]
            }
        }
    }.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds = _recordingDurationSeconds.asStateFlow()

    private val _waveformBars = MutableStateFlow(List(6) { 10f })
    val waveformBars = _waveformBars.asStateFlow()

    private val _hasRecorded = MutableStateFlow(false)
    val hasRecorded = _hasRecorded.asStateFlow()

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating = _isEvaluating.asStateFlow()

    private val _evaluationResult = MutableStateFlow<EvaluationResult?>(null)
    val evaluationResult = _evaluationResult.asStateFlow()

    private var recordingJob: Job? = null
    private var waveformJob: Job? = null

    // ─── Profile states (from linguaflow2) ───────────────────────────────────
    private val _profileName = MutableStateFlow("Bima Melayu")
    val profileName = _profileName.asStateFlow()

    private val _profileUsername = MutableStateFlow("@bimamelayu")
    val profileUsername = _profileUsername.asStateFlow()

    private val _profileEmail = MutableStateFlow("bima.melayu@example.com")
    val profileEmail = _profileEmail.asStateFlow()

    private val _profileBio = MutableStateFlow("")
    val profileBio = _profileBio.asStateFlow()

    private val _learningTarget = MutableStateFlow("Normal")
    val learningTarget = _learningTarget.asStateFlow()

    private val _languagesList = MutableStateFlow(listOf("Jepang", "Inggris"))
    val languagesList = _languagesList.asStateFlow()

    val tempName = MutableStateFlow("Bima Melayu")
    val tempUsername = MutableStateFlow("@bimamelayu")
    val tempBio = MutableStateFlow("")
    val tempTarget = MutableStateFlow("Normal")
    val tempLanguages = MutableStateFlow(listOf("Jepang", "Inggris"))

    // ─── Init ─────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            delay(2800)
            _currentScreen.value = AppScreen.ONBOARDING
        }
    }

    // ─── Navigation functions ─────────────────────────────────────────────────
    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun setUserName(name: String) {
        _userName.value = if (name.trim().isEmpty()) "Tamu" else name
        _profileName.value = if (name.trim().isEmpty()) "Tamu" else name
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        _themeMode.value = when (theme) {
            AppTheme.LIGHT -> "Terang"
            AppTheme.DARK -> "Gelap"
            AppTheme.SYSTEM -> "Sistem"
        }
    }

    fun selectWordForDetail(word: Word?) {
        _selectedWordForDetail.value = word
    }

    fun toggleBookmark(word: Word) {
        viewModelScope.launch(Dispatchers.IO) {
            wordDao.updateBookmark(word.id, !word.bookmarked)
            if (_selectedWordForDetail.value?.id == word.id) {
                _selectedWordForDetail.value = word.copy(bookmarked = !word.bookmarked)
            }
        }
    }

    fun markAsLearned(word: Word, learned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            wordDao.updateLearned(word.id, learned)
        }
    }

    // ─── Flashcard functions ──────────────────────────────────────────────────
    fun startFlashcardSession(deckCategory: String) {
        viewModelScope.launch {
            val list = allWords.value.filter { it.category == deckCategory || deckCategory == "Semua" }
            if (list.isNotEmpty()) {
                _activeFlashcards.value = list.shuffled()
                _currentFlashcardIndex.value = 0
                _isCardFlipped.value = false
                _currentScreen.value = AppScreen.FLASHCARD
            }
        }
    }

    fun nextFlashcard() {
        if (_currentFlashcardIndex.value < _activeFlashcards.value.size - 1) {
            _currentFlashcardIndex.value += 1
            _isCardFlipped.value = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val ph = PracticeHistory(
                    title = "Flashcard: Sesi Selesai",
                    score = 100,
                    category = "Kuis Kosakata"
                )
                practiceHistoryDao.insertHistory(ph)
            }
            _currentScreen.value = AppScreen.MAIN
        }
    }

    fun prevFlashcard() {
        if (_currentFlashcardIndex.value > 0) {
            _currentFlashcardIndex.value -= 1
            _isCardFlipped.value = false
        }
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    // ─── Quiz functions ───────────────────────────────────────────────────────
    fun startQuizSession() {
        val list = allWords.value
        if (list.isNotEmpty()) {
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _quizCorrectState.value = null
            _quizSelectedWordChips.value = emptyList()
            _currentScreen.value = AppScreen.QUIZ
        }
    }

    fun toggleWordChip(word: String) {
        if (_quizSelectedWordChips.value.contains(word)) {
            _quizSelectedWordChips.value = _quizSelectedWordChips.value.filter { it != word }
        } else {
            _quizSelectedWordChips.value = _quizSelectedWordChips.value + word
        }
    }

    fun playQuizAudio() {
        viewModelScope.launch {
            _quizAudioPlaying.value = true
            delay(3000)
            _quizAudioPlaying.value = false
        }
    }

    fun submitQuizAnswer() {
        val activeQuizIndex = _currentQuizIndex.value
        val answer = _quizSelectedWordChips.value.joinToString(" ").lowercase()

        val isCorrect = when (activeQuizIndex) {
            0 -> answer == "watashi wa sushi o tabemasu"
            1 -> answer == "watashi wa gakusei desu"
            else -> answer.contains("sushi") || answer.contains("tabemasu")
        }

        _quizCorrectState.value = isCorrect
        if (isCorrect) {
            _quizScore.value += 20
        }
    }

    fun nextQuizStep() {
        if (_currentQuizIndex.value < 2) {
            _currentQuizIndex.value += 1
            _quizCorrectState.value = null
            _quizSelectedWordChips.value = emptyList()
        } else {
            val finalScore = (_quizScore.value * 100) / 60
            viewModelScope.launch(Dispatchers.IO) {
                val ph = PracticeHistory(
                    title = "Kuis Tata Bahasa & Kosa Kata",
                    score = finalScore,
                    category = "Tata Bahasa"
                )
                practiceHistoryDao.insertHistory(ph)
            }
            _currentScreen.value = AppScreen.MAIN
            _activeTab.value = MainTab.LATIHAN
        }
    }

    // ─── AI Sensei chat functions ─────────────────────────────────────────────
    fun toggleAiSensei(expanded: Boolean) {
        _aiSenseiExpanded.value = expanded
    }

    fun sendQuestionToSensei(question: String) {
        if (question.trim().isEmpty()) return

        val userMsg = ChatMessage(question, isUser = true)
        _aiChatMessages.value = _aiChatMessages.value + userMsg

        _isAiThinking.value = true
        viewModelScope.launch {
            val response = GeminiClient.askSensei(question)
            val senseiMsg = ChatMessage(response, isUser = false)
            _aiChatMessages.value = _aiChatMessages.value + senseiMsg
            _isAiThinking.value = false
        }
    }

    // ─── Speech practice functions (from linguaflow2) ─────────────────────────
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _hasRecorded.value = false
        _recordingDurationSeconds.value = 0

        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }

        waveformJob = viewModelScope.launch {
            while (_isRecording.value) {
                _waveformBars.value = List(6) { (15..80).random().toFloat() }
                delay(150)
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        waveformJob?.cancel()
        _waveformBars.value = List(6) { 10f }
        _hasRecorded.value = true
    }

    fun clearRecording() {
        stopRecording()
        _hasRecorded.value = false
        _recordingDurationSeconds.value = 0
        _evaluationResult.value = null
    }

    fun submitForEvaluation(context: Context, navigateToResult: () -> Unit) {
        val target = currentExercise.value
        _isEvaluating.value = true
        _evaluationResult.value = null

        viewModelScope.launch {
            val spokenText = if (target.id == 1) {
                "私は毎日日本語をべんきょうします"
            } else {
                target.text
            }

            val result = GeminiClient.evaluateSpeech(target.text, spokenText)
            _evaluationResult.value = result
            _isEvaluating.value = false

            val dateText = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID")).format(Date())
            val durationMinSec = String.format(Locale.getDefault(), "0:%02d", _recordingDurationSeconds.value.coerceAtLeast(3))
            val wordsEvaluationString = result.wordEvaluations.joinToString(",") { "${it.word}:${it.score}" }

            val historyItem = SpeechHistory(
                dateText = dateText,
                timestamp = System.currentTimeMillis(),
                durationText = durationMinSec,
                targetSentence = target.text,
                romaji = target.romaji,
                overallScore = result.overallScore,
                clarityScore = result.clarityScore,
                intonationScore = result.intonationScore,
                fluencyScore = result.fluencyScore,
                feedback = result.feedback,
                wordsEvaluations = wordsEvaluationString
            )

            launch(Dispatchers.IO) {
                speechHistoryDao.insertHistory(historyItem)
            }

            navigateToResult()
        }
    }

    fun nextExercise() {
        _currentExerciseIndex.value += 1
        clearRecording()
    }

    // ─── Profile functions (from linguaflow2) ────────────────────────────────
    fun startEditingProfile() {
        tempName.value = _profileName.value
        tempUsername.value = _profileUsername.value
        tempBio.value = _profileBio.value
        tempTarget.value = _learningTarget.value
        tempLanguages.value = _languagesList.value
    }

    fun saveProfile() {
        _profileName.value = tempName.value
        _profileUsername.value = tempUsername.value
        _profileBio.value = tempBio.value
        _learningTarget.value = tempTarget.value
        _languagesList.value = tempLanguages.value
    }
}
