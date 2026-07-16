package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.EvaluationResult
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.PracticeHistory
import com.example.data.SpeechHistory
import com.example.data.UserProgress
import com.example.data.Word
import com.example.data.repository.PracticeRepository
import com.example.data.repository.SpeechRepository
import com.example.data.repository.UserProgressRepository
import com.example.data.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

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

@HiltViewModel
class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val wordRepository = WordRepository(db.wordDao())
    private val practiceRepository = PracticeRepository(db.practiceHistoryDao())
    private val speechRepository = SpeechRepository(db.speechHistoryDao())
    private val userProgressRepository = UserProgressRepository(db.userProgressDao())

    // ─── DB flows (via repository layer) ───────────────────────────────────
    val allWords: StateFlow<List<Word>> = wordRepository.getAllWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val histories: StateFlow<List<PracticeHistory>> = practiceRepository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechHistory: StateFlow<List<SpeechHistory>> = speechRepository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProgress: StateFlow<UserProgress?> = userProgressRepository.getProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private val _speechAvailable = MutableStateFlow(true)
    val speechAvailable = _speechAvailable.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError = _speechError.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

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
            wordRepository.updateBookmark(word.id, !word.bookmarked)
            if (_selectedWordForDetail.value?.id == word.id) {
                _selectedWordForDetail.value = word.copy(bookmarked = !word.bookmarked)
            }
        }
    }

    fun markAsLearned(word: Word, learned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            wordRepository.updateLearned(word.id, learned)
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
                practiceRepository.insertHistory(ph)
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
                practiceRepository.insertHistory(ph)
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
    fun toggleRecording(context: Context) {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording(context)
        }
    }

    fun setSpeechError(message: String) {
        _speechError.value = message
    }

    private fun startRecording(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _speechError.value = "Izin mikrofon diperlukan untuk latihan ucapan."
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechAvailable.value = false
            _speechError.value = "Speech recognition tidak tersedia di perangkat/emulator ini."
            return
        }

        _speechError.value = null
        _recognizedText.value = ""
        _isRecording.value = true
        _hasRecorded.value = false
        _recordingDurationSeconds.value = 0

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                _speechError.value = "Gagal mengenali ucapan (kode $error)."
                stopRecording()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _recognizedText.value = matches?.firstOrNull()?.trim() ?: ""
                stopRecording()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { _recognizedText.value = it.trim() }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)

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
        speechRecognizer?.stopListening()
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
        val transcript = _recognizedText.value.trim()
        if (transcript.isEmpty()) {
            _speechError.value = "Belum ada ucapan terdeteksi. Tekan mikrofon lalu ucapkan kalimatnya."
            return
        }

        val target = currentExercise.value
        _isEvaluating.value = true
        _evaluationResult.value = null

        viewModelScope.launch {
            val result = GeminiClient.evaluateSpeech(target.text, transcript)
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
                speechRepository.insertHistory(historyItem)
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

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
