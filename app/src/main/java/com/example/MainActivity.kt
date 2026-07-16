package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.AppTheme
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SoftLavenderBg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeState by viewModel.appTheme.collectAsState()
            val isDarkTheme = when (themeState) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val currentScreen by viewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.SPLASH -> {
                            SplashScreen()
                        }
                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(viewModel = viewModel)
                        }
                        AppScreen.MAIN -> {
                            MainHubScreen(viewModel = viewModel)
                        }
                        AppScreen.FLASHCARD -> {
                            FlashcardScreen(viewModel = viewModel)
                        }
                        AppScreen.QUIZ -> {
                            QuizScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val topBarTitle = when (activeTab) {
        MainTab.BELAJAR -> "Konnichiwa, $userName! 👋"
        MainTab.LATIHAN -> "Latihan Harian 🏆"
        MainTab.KAMUS -> "Kamus Kosakata N5 📖"
        MainTab.PERINGKAT -> "Peringkat Liga ⚡"
        MainTab.UCAPAN -> "Latihan Ucapan 🎤"
        MainTab.PROFIL -> "Profil Pengguna 👤"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SoftLavenderBg
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SoftLavenderBg,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Belajar Tab
                NavigationBarItem(
                    selected = activeTab == MainTab.BELAJAR,
                    onClick = { viewModel.setTab(MainTab.BELAJAR) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Belajar Tab Icon"
                        )
                    },
                    label = { Text("Belajar", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // Latihan Tab
                NavigationBarItem(
                    selected = activeTab == MainTab.LATIHAN,
                    onClick = { viewModel.setTab(MainTab.LATIHAN) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Latihan Tab Icon"
                        )
                    },
                    label = { Text("Latihan", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // Kamus Tab
                NavigationBarItem(
                    selected = activeTab == MainTab.KAMUS,
                    onClick = { viewModel.setTab(MainTab.KAMUS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Kamus Tab Icon"
                        )
                    },
                    label = { Text("Kamus", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // Peringkat Tab
                NavigationBarItem(
                    selected = activeTab == MainTab.PERINGKAT,
                    onClick = { viewModel.setTab(MainTab.PERINGKAT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Peringkat Tab Icon"
                        )
                    },
                    label = { Text("Liga", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // Ucapan Tab (NEW - from linguaflow2)
                NavigationBarItem(
                    selected = activeTab == MainTab.UCAPAN,
                    onClick = { viewModel.setTab(MainTab.UCAPAN) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Ucapan Tab Icon"
                        )
                    },
                    label = { Text("Ucapan", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // Profil Tab
                NavigationBarItem(
                    selected = activeTab == MainTab.PROFIL,
                    onClick = { viewModel.setTab(MainTab.PROFIL) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil Tab Icon"
                        )
                    },
                    label = { Text("Profil", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                MainTab.BELAJAR -> {
                    LearnTab(viewModel = viewModel)
                }
                MainTab.LATIHAN -> {
                    PracticeTab(viewModel = viewModel)
                }
                MainTab.KAMUS -> {
                    KamusTab(viewModel = viewModel)
                }
                MainTab.PERINGKAT -> {
                    PeringkatTab(viewModel = viewModel)
                }
                MainTab.UCAPAN -> {
                    // Speech practice screens from linguaflow2
                    SpeechSectionTab(viewModel = viewModel)
                }
                MainTab.PROFIL -> {
                    ProfilTab(viewModel = viewModel)
                }
            }

            // Overlay Floating AI Sensei chat views
            AiSenseiView(viewModel = viewModel)

            // Overlays the word detail dialog if active
            WordDetailSheet(viewModel = viewModel)
        }
    }
}
