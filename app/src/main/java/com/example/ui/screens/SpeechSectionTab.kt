package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.MainViewModel

/**
 * SpeechSectionTab — Wrapper composable untuk menampilkan fitur latihan ucapan
 * dari linguaflow2 sebagai tab dalam aplikasi linguaflow yang digabungkan.
 *
 * Tab ini menampung navigasi internal: quiz_selection → speech_practice → evaluation_result
 * → speech_history, profile, statistics, settings (dari ZenithLinguaScreens.kt).
 * Sub-navigation bar muncul hanya di layar utama (bukan saat recording/evaluasi).
 */
@Composable
fun SpeechSectionTab(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("quiz_selection") }
    val tts = remember { AppTextToSpeech(context) }

    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
        }
    }

    val showSubNav = currentScreen in listOf(
        "quiz_selection", "speech_history", "profile", "statistics", "settings"
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    )
   { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "quiz_selection" -> QuizSelectionScreen(
                    onNavigateToSpeech = {
                        viewModel.clearRecording()
                        currentScreen = "speech_practice"
                    },
                    onNavigate = { screen -> currentScreen = screen }
                )
                "speech_practice" -> SpeechPracticeScreen(
                    viewModel = viewModel,
                    tts = tts,
                    onBack = { currentScreen = "quiz_selection" },
                    onNavigateToResult = { currentScreen = "evaluation_result" }
                )
                "evaluation_result" -> EvaluationResultScreen(
                    viewModel = viewModel,
                    tts = tts,
                    onTryAgain = {
                        viewModel.clearRecording()
                        currentScreen = "speech_practice"
                    },
                    onNext = {
                        viewModel.nextExercise()
                        currentScreen = "speech_practice"
                    },
                    onClose = { currentScreen = "quiz_selection" }
                )
                "speech_history" -> SpeechHistoryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "quiz_selection" },
                    onNavigate = { screen -> currentScreen = screen }
                )
                "profile" -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToEdit = {
                        viewModel.startEditingProfile()
                        currentScreen = "edit_profile"
                    },
                    onNavigateToHistory = { currentScreen = "speech_history" },
                    onNavigateToStats = { currentScreen = "statistics" },
                    onNavigateToSettings = { currentScreen = "settings" },
                    onNavigate = { screen -> currentScreen = screen }
                )
                "edit_profile" -> EditProfileScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "profile" }
                )
                "statistics" -> StatisticsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "profile" },
                    onNavigate = { screen -> currentScreen = screen }
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "profile" },
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        }
    }
}
