package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.EvaluationResult
import com.example.api.WordEvaluation
import com.example.data.SpeechHistory
import com.example.ui.theme.*
import com.example.ui.MainViewModel
import java.util.Locale

// TTS wrapper to speak phrases aloud
class AppTextToSpeech(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
                isReady = true
            }
        }
    }

    fun speak(text: String, locale: Locale = Locale.JAPANESE) {
        if (isReady) {
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

@Composable
fun MainAppNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("quiz_selection") }
    val tts = remember { AppTextToSpeech(context) }

    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen in listOf("quiz_selection", "speech_history", "profile", "statistics", "settings")) {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
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

// ==========================================
// 1. SELECT QUIZ SCREEN
// ==========================================
@Composable
fun QuizSelectionScreen(
    onNavigateToSpeech: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(
                text = "Pilih Kuis",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pilih jenis latihan yang ingin kamu kerjakan hari ini",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Grid/Column of customized cards matching the UI
        QuizCard(
            title = "Kuis Kosakata",
            description = "Uji pemahamanmu terhadap arti dan penggunaan kata",
            statusText = "Terakhir: 82%",
            statusColor = GreenAccent,
            statusBg = Emerald50,
            icon = Icons.Default.Book,
            iconColor = MaterialTheme.colorScheme.primary,
            bgColor = Color(0xFFEEEDFE),
            footerText = "48 soal tersedia",
            onClick = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        QuizCard(
            title = "Kuis Kanji",
            description = "Kenali karakter kanji dan cara bacanya",
            statusText = "Terakhir: 68%",
            statusColor = YellowAccent,
            statusBg = Amber50,
            icon = Icons.Default.Brush,
            iconColor = Color(0xFFB45309),
            bgColor = Color(0xFFFAEEDA),
            footerText = "30 soal",
            onClick = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        QuizCard(
            title = "Kuis Tata Bahasa",
            description = "Latih kemampuan menyusun kalimat yang benar",
            statusText = "Belum pernah",
            statusColor = Color.Gray,
            statusBg = Color(0xFFF3F3F3),
            icon = Icons.Default.Create,
            iconColor = Color(0xFF059669),
            bgColor = Color(0xFFE1F5EE),
            footerText = "25 soal",
            onClick = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        QuizCard(
            title = "Latihan Ucapan",
            description = "Latih pengucapan dengan penilaian AI secara real-time",
            statusText = "Terakhir: 74%",
            statusColor = YellowAccent,
            statusBg = Amber50,
            icon = Icons.Default.Mic,
            iconColor = Color(0xFFDC2626),
            bgColor = Color(0xFFFAECE7),
            footerText = "10 soal",
            testTag = "quiz_speech_practice_card",
            onClick = onNavigateToSpeech
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuizCard(
    title: String,
    description: String,
    statusText: String,
    statusColor: Color,
    statusBg: Color,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    footerText: String,
    testTag: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = footerText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ==========================================
// 2. SPEECH PRACTICE SCREEN (RECORDING)
// ==========================================
@Composable
fun SpeechPracticeScreen(
    viewModel: MainViewModel,
    tts: AppTextToSpeech,
    onBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val context = LocalContext.current
    val exercise by viewModel.currentExercise.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.recordingDurationSeconds.collectAsStateWithLifecycle()
    val waveformBars by viewModel.waveformBars.collectAsStateWithLifecycle()
    val hasRecorded by viewModel.hasRecorded.collectAsStateWithLifecycle()
    val isEvaluating by viewModel.isEvaluating.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val speechAvailable by viewModel.speechAvailable.collectAsStateWithLifecycle()

    val recordPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.toggleRecording(context)
        else viewModel.setSpeechError("Izin mikrofon ditolak. Tidak bisa merekam.")
    }

    val formattedTime = String.format(Locale.getDefault(), "00:%02d", durationSeconds)

    if (isEvaluating) {
        EvaluatingProgressOverlay()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
                Text(
                    text = "Latihan Ucapan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = exercise.indexText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { 0.3f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // Sentence Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ucapkan kalimat berikut:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = exercise.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exercise.romaji,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { tts.speak(exercise.text) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Dengar contoh", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Mic area
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing light circle animation when recording
                if (isRecording) {
                    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "micScale"
                    )
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "micAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
                    )
                }

                // Main circular mic button
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.toggleRecording(context)
                        } else {
                            recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .testTag("mic_button")
                        .clip(CircleShape)
                        .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Waveform visual feedback
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                waveformBars.forEach { heightVal ->
                    val animatedHeight by animateFloatAsState(
                        targetValue = if (isRecording) heightVal else 8f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "bar"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(4.dp)
                            .height(animatedHeight.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRecording) formattedTime else if (hasRecorded) "Selesai merekam!" else "00:00",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRecording) "Merekam suara..." else "Tekan mikrofon dan mulai berbicara",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            speechError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit / Redo buttons
            Button(
                onClick = { viewModel.submitForEvaluation(context, onNavigateToResult) },
                enabled = (hasRecorded || durationSeconds > 0) && speechAvailable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.outline
                ),
                shape = CircleShape
            ) {
                Text(text = "Kirim Jawaban", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { viewModel.clearRecording() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ulangi",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun EvaluatingProgressOverlay() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Menilai dengan AI...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gemini sedang mengevaluasi ketepatan pengucapan dan intonasi Anda",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ==========================================
// 3. EVALUATION RESULT SCREEN
// ==========================================
@Composable
fun EvaluationResultScreen(
    viewModel: MainViewModel,
    tts: AppTextToSpeech,
    onTryAgain: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val result by viewModel.evaluationResult.collectAsStateWithLifecycle()
    val exercise by viewModel.currentExercise.collectAsStateWithLifecycle()
    var selectedWordDetail by remember { mutableStateOf<WordEvaluation?>(null) }

    val eval = result ?: return // Guard check

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                text = "Hasil Penilaian",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp)) // Offset back arrow
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Overall Score gauge card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Progress Canvas
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val strokeWidth = 14.dp
                        val colorPrimary = MaterialTheme.colorScheme.primary
                        val colorTrack = MaterialTheme.colorScheme.secondaryContainer

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Track
                            drawArc(
                                color = colorTrack,
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                            )
                            // Fill
                            drawArc(
                                color = colorPrimary,
                                startAngle = -220f,
                                sweepAngle = 260f * (eval.overallScore / 100f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${eval.overallScore}%",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pengucapan",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Subscores Grid Row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SubScoreColumn(
                            title = "Kejelasan",
                            score = eval.clarityScore,
                            modifier = Modifier.weight(1f)
                        )
                        SubScoreColumn(
                            title = "Intonasi",
                            score = eval.intonationScore,
                            modifier = Modifier.weight(1f)
                        )
                        SubScoreColumn(
                            title = "Kelancaran",
                            score = eval.fluencyScore,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Target Sentence Replay
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Kalimat Target", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = exercise.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { tts.speak(exercise.text) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play target",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Recording card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { tts.speak(exercise.text) }, // Simulate replaying user
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play recording",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Rekaman Kamu", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        // Mock waveform visual
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        ) {
                            val mockHeights = listOf(12, 20, 8, 14, 24, 6, 18, 10, 16, 8)
                            mockHeights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .width(3.dp)
                                        .height(h.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "0:04", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Word Analysis
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Analisis Per Kata",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Flow of word chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        eval.wordEvaluations.forEach { wordEval ->
                            val (bg, border, text) = when (wordEval.score) {
                                2 -> Triple(Emerald50, GreenAccent, Color(0xFF065F46))
                                1 -> Triple(Amber50, YellowAccent, Color(0xFF92400E))
                                else -> Triple(Red50, RedAccent, Color(0xFF991B1B))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(8.dp))
                                    .clickable { selectedWordDetail = wordEval }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = wordEval.word,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ketuk kata untuk detail pengucapan",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Suggestion tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .drawLeftBorder(color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Tip",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = eval.feedback,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                OutlinedButton(
                    onClick = onTryAgain,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "Coba Lagi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("next_button"),
                    shape = CircleShape
                ) {
                    Text(text = "Soal Berikutnya", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    // Detail dialog for clicked word chip
    selectedWordDetail?.let { wordEval ->
        val scoreLabel = when (wordEval.score) {
            2 -> "Sempurna!"
            1 -> "Cukup Baik"
            else -> "Butuh Perbaikan"
        }
        val scoreColor = when (wordEval.score) {
            2 -> GreenAccent
            1 -> YellowAccent
            else -> RedAccent
        }

        Dialog(onDismissRequest = { selectedWordDetail = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Detail Kata", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = wordEval.word, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = scoreLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { selectedWordDetail = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Tutup")
                    }
                }
            }
        }
    }
}

@Composable
fun SubScoreColumn(
    title: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$score%",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Progress bar
        LinearProgressIndicator(
            progress = { score / 100f },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(6.dp)
                .clip(CircleShape)
        )
    }
}

// Simple custom modifier border extension
fun Modifier.drawLeftBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp): Modifier {
    return this.drawBehind {
        val width = strokeWidth.toPx()
        drawLine(
            color = color,
            start = Offset(width / 2f, 0f),
            end = Offset(width / 2f, size.height),
            strokeWidth = width
        )
    }
}

// ==========================================
// 4. SPEECH HISTORY SCREEN
// ==========================================
@Composable
fun SpeechHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val historyList by viewModel.speechHistory.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("Semua") } // Semua, Skor Tinggi, Perlu Latihan

    val filteredList = remember(historyList, selectedFilter) {
        when (selectedFilter) {
            "Skor Tinggi" -> historyList.filter { it.overallScore >= 80 }
            "Perlu Latihan" -> historyList.filter { it.overallScore < 70 }
            else -> historyList
        }
    }

    val avgScore = remember(historyList) {
        if (historyList.isEmpty()) 76 else historyList.map { it.overallScore }.average().toInt()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Custom header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "Histori Ucapan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) // balances back button
            }
        }

        // Summary Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsSummaryCard(
                    label = "Total sesi",
                    value = "${historyList.size}",
                    modifier = Modifier.weight(1f)
                )
                StatsSummaryCard(
                    label = "Rata-rata skor",
                    value = "$avgScore%",
                    modifier = Modifier.weight(1f)
                )
                StatsSummaryCard(
                    label = "Streak ucapan",
                    value = "5 hari",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Line Chart Canvas drawing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Progres 14 Hari Terakhir",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw a gorgeous line graph inside Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val chartPrimary = MaterialTheme.colorScheme.primary
                        val chartPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw horizontal grid lines
                            drawLine(Color(0xFFEEEEEE), Offset(0f, 0f), Offset(w, 0f), strokeWidth = 2f)
                            drawLine(Color(0xFFEEEEEE), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 2f)
                            drawLine(Color(0xFFEEEEEE), Offset(0f, h), Offset(w, h), strokeWidth = 4f)

                            // Point coordinates (hardcoded simulated points matching trend)
                            val points = listOf(
                                Offset(w * 0.05f, h * 0.75f),
                                Offset(w * 0.20f, h * 0.70f),
                                Offset(w * 0.35f, h * 0.85f),
                                Offset(w * 0.50f, h * 0.60f),
                                Offset(w * 0.65f, h * 0.45f),
                                Offset(w * 0.80f, h * 0.52f),
                                Offset(w * 0.95f, h * 0.25f)
                            )

                            // Draw trend line
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = chartPrimaryContainer,
                                    start = points[i],
                                    end = points[i + 1],
                                    strokeWidth = 8f,
                                    cap = StrokeCap.Round
                                )
                            }

                            // Draw data points
                            points.forEach { pt ->
                                drawCircle(color = chartPrimary, radius = 10f, center = pt)
                                drawCircle(color = Color.White, radius = 5f, center = pt)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "10 Jun", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "17 Jun", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "23 Jun", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Filter chips row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "Skor Tinggi", "Perlu Latihan").forEach { filter ->
                    val isActive = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // History items list
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Belum ada riwayat ucapan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredList) { item ->
                HistoryListItemCard(item = item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HistoryListItemCard(item: SpeechHistory) {
    val context = LocalContext.current
    val strokeColor = when {
        item.overallScore >= 80 -> GreenAccent
        item.overallScore >= 70 -> YellowAccent
        else -> RedAccent
    }
    val badgeBg = when {
        item.overallScore >= 80 -> Emerald50
        item.overallScore >= 70 -> Amber50
        else -> Red50
    }
    val badgeText = when {
        item.overallScore >= 80 -> Color(0xFF166534)
        item.overallScore >= 70 -> Color(0xFF854D0E)
        else -> Color(0xFF991B1B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .drawLeftBorder(color = strokeColor, strokeWidth = 4.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Info row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = item.dateText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "â€¢", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = item.durationText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.targetSentence,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mini waveform animation placeholder
                    Row(
                        modifier = Modifier.height(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(8, 14, 6, 12, 16, 4).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(h.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.overallScore}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Memutar kembali rekaman: ${item.targetSentence}", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Putar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatsSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================
// 5. PROFILE SCREEN
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val name by viewModel.profileName.collectAsStateWithLifecycle()
    val username by viewModel.profileUsername.collectAsStateWithLifecycle()
    val bio by viewModel.profileBio.collectAsStateWithLifecycle()
    val learningTarget by viewModel.learningTarget.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = { onNavigate("quiz_selection") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "Profil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Hero profile section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F3FF))
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BM Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (name.length >= 2) name.substring(0, 2).uppercase() else "BM",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFDDD6FE))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Level 12 â€¢ Menengah",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNavigateToEdit) {
                Text(
                    text = "Edit Profil",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Stats boxes row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-24).dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStatCard(
                icon = Icons.Default.Whatshot,
                iconColor = Color(0xFFF97316),
                title = "14 Hari",
                subtitle = "STREAK",
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                icon = Icons.Default.Star,
                iconColor = Color(0xFFFACC15),
                title = "4.820 XP",
                subtitle = "TOTAL XP",
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                icon = Icons.Default.MenuBook,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "312 Kata",
                subtitle = "DIKUASAI",
                modifier = Modifier.weight(1f)
            )
        }

        // Lencana Terbaru (Achievements) Horizontal Scroll
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Lencana Terbaru", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { Toast.makeText(context, "Fitur Lencana Lengkap segera hadir!", Toast.LENGTH_SHORT).show() }) {
                    Text(text = "Lihat semua", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { BadgeIconItem(title = "Awal Mula", icon = Icons.Default.WorkspacePremium, iconColor = Color(0xFFF97316), borderGradient = listOf(Color(0xFFFCD34D), Color(0xFFFB923C))) }
                item { BadgeIconItem(title = "Melesat", icon = Icons.Default.RocketLaunch, iconColor = MaterialTheme.colorScheme.primary, borderGradient = listOf(Color(0xFFC084FC), MaterialTheme.colorScheme.primary)) }
                item { BadgeIconItem(title = "Fokus", icon = Icons.Default.Psychology, iconColor = Color.Gray, isLocked = true) }
                item { BadgeIconItem(title = "Kreatif", icon = Icons.Default.AutoAwesome, iconColor = Color(0xFF3B82F6), borderGradient = listOf(Color(0xFF93C5FD), Color(0xFF3B82F6))) }
                item { BadgeIconItem(title = "Locked", icon = Icons.Default.Lock, iconColor = Color.LightGray, isLocked = true) }
            }
        }

        // Kursus Aktif
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(text = "Kursus Aktif", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))

            ActiveCourseProgressCard(levelName = "N5", courseTitle = "Japanese JLPT N5", progressPercent = 85, progressColor = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            ActiveCourseProgressCard(levelName = "N4", courseTitle = "Japanese JLPT N4", progressPercent = 12, progressColor = MaterialTheme.colorScheme.secondary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Menu list items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            ProfileMenuItem(icon = Icons.Default.BarChart, label = "Statistik Detail", onClick = onNavigateToStats)
            ProfileMenuItem(icon = Icons.Default.History, label = "Riwayat Belajar", onClick = onNavigateToHistory)
            ProfileMenuItem(icon = Icons.Default.Settings, label = "Pengaturan", onClick = onNavigateToSettings)
            ProfileMenuItem(icon = Icons.Default.Help, label = "Bantuan", onClick = { Toast.makeText(context, "Hubungi bantuan di support@linguaflow.com", Toast.LENGTH_LONG).show() })
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { Toast.makeText(context, "Sampai jumpa kembali, Bima!", Toast.LENGTH_LONG).show() }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Keluar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileStatCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BadgeIconItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    borderGradient: List<Color> = emptyList(),
    isLocked: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isLocked) Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerLow))
                    else if (borderGradient.isNotEmpty()) Brush.sweepGradient(borderGradient) 
                    else Brush.linearGradient(listOf(Color.White, Color.White))
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLocked) Color.LightGray else iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ActiveCourseProgressCard(
    levelName: String,
    courseTitle: String,
    progressPercent: Int,
    progressColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(progressColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = levelName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = progressColor)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(text = courseTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$progressPercent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = progressColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ==========================================
// 6. EDIT PROFILE SCREEN
// ==========================================
@Composable
fun EditProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val tempName by viewModel.tempName.collectAsStateWithLifecycle()
    val tempUsername by viewModel.tempUsername.collectAsStateWithLifecycle()
    val tempBio by viewModel.tempBio.collectAsStateWithLifecycle()
    val tempTarget by viewModel.tempTarget.collectAsStateWithLifecycle()
    val tempLanguages by viewModel.tempLanguages.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            TextButton(onClick = onBack) {
                Text(text = "Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            Text(
                text = "Edit Profil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = {
                    viewModel.saveProfile()
                    onBack()
                }
            ) {
                Text(text = "Simpan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Avatar section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (tempName.length >= 2) tempName.substring(0, 2).uppercase() else "BM",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.clickable { },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Ganti Foto", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Fields column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Full name
            Column {
                Text(text = "Nama Lengkap", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { viewModel.tempName.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Username
            Column {
                Text(text = "Username", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempUsername,
                    onValueChange = { viewModel.tempUsername.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Email (Read-only)
            Column {
                Text(text = "Email", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = "bima.melayu@example.com",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = "Verified", tint = GreenAccent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Bio Text area
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Bio", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${tempBio.length}/120", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = tempBio,
                    onValueChange = { if (it.length <= 120) viewModel.tempBio.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(text = "Tuliskan sesuatu tentang dirimu...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Target Belajar Section
            Column {
                Text(text = "Target Belajar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                LearningTargetRadioButton(
                    title = "Santai",
                    description = "5 menit per hari",
                    isActive = tempTarget == "Santai",
                    onClick = { viewModel.tempTarget.value = "Santai" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LearningTargetRadioButton(
                    title = "Normal",
                    description = "15 menit per hari",
                    isActive = tempTarget == "Normal",
                    onClick = { viewModel.tempTarget.value = "Normal" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LearningTargetRadioButton(
                    title = "Intensif",
                    description = "30 menit per hari",
                    isActive = tempTarget == "Intensif",
                    onClick = { viewModel.tempTarget.value = "Intensif" }
                )
            }

            // Languages Section
            Column {
                Text(text = "Bahasa yang Dipelajari", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tempLanguages.forEach { lang ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = lang, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            viewModel.tempLanguages.value = tempLanguages.filter { it != lang }
                                        }
                                )
                            }
                        }
                    }

                    // Add button
                    IconButton(
                        onClick = {
                            if (!tempLanguages.contains("Mandarin")) {
                                viewModel.tempLanguages.value = tempLanguages + "Mandarin"
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LearningTargetRadioButton(
    title: String,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFFF5F3FF) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Radio dot
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 2.dp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. STATISTICS SCREEN
// ==========================================
@Composable
fun StatisticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("7 Hari") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "Statistik",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Time switchers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("7 Hari", "30 Hari", "Semua").forEach { tab ->
                val isActive = selectedTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth(0.6f)
                            .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Activity Bar Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(text = "AKTIVITAS MINGGUAN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "1,240 XP", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "+12%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(text = "vs minggu lalu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bouncing/Grid bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barWeights = listOf(
                            Pair("Sen", 0.55f),
                            Pair("Sel", 0.70f),
                            Pair("Rab", 0.40f),
                            Pair("Kam", 0.85f),
                            Pair("Jum", 0.95f), // Active Friday
                            Pair("Sab", 0.65f),
                            Pair("Min", 0.50f)
                        )

                        barWeights.forEach { bar ->
                            val isActive = bar.first == "Jum"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(bar.second)
                                        .width(32.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bar.first,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Vocabulary Donut Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Penguasaan Kosakata",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Canvas Donut
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val donutPrimary = MaterialTheme.colorScheme.primary
                        val donutPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 12.dp.toPx()
                            // Gray background
                            drawCircle(
                                color = Color(0xFFF3F3F3),
                                radius = size.minDimension / 2f - stroke / 2f,
                                style = Stroke(width = stroke)
                            )
                            // Mastery (68%)
                            drawArc(
                                color = donutPrimary,
                                startAngle = -90f,
                                sweepAngle = 360f * 0.68f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // Learning (20%)
                            drawArc(
                                color = donutPrimaryContainer,
                                startAngle = -90f + (360f * 0.68f),
                                sweepAngle = 360f * 0.20f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "68%", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text(text = "DIKUASAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = MaterialTheme.colorScheme.primary, value = "612", label = "Dikuasai")
                        LegendItem(color = MaterialTheme.colorScheme.primaryContainer, value = "180", label = "Belajar")
                        LegendItem(color = MaterialTheme.colorScheme.surfaceContainerHighest, value = "108", label = "Belum")
                    }
                }
            }

            // Heatmap Aktivitas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Heatmap Aktivitas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "3 Bulan Terakhir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated grid rows of heatmap squares
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // We display 15 column blocks
                        for (colIndex in 1..15) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (rowIndex in 1..7) {
                                    val intensity = (0..4).random()
                                    val squareColor = when (intensity) {
                                        4 -> MaterialTheme.colorScheme.primary
                                        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        2 -> MaterialTheme.colorScheme.primaryContainer
                                        1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(squareColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scale helper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Kurang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primaryContainer))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Banyak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Summary Perf
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Ringkasan Performa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        PerfSummaryRow(icon = Icons.Default.Schedule, label = "Sesi belajar", value = "48 sesi")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        PerfSummaryRow(icon = Icons.Default.HourglassTop, label = "Rata-rata durasi", value = "18 menit")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        PerfSummaryRow(icon = Icons.Default.Bolt, label = "Hari terpanjang", value = "52 menit")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        PerfSummaryRow(icon = Icons.Default.ListAlt, label = "Kuis diselesaikan", value = "120 kuis")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        PerfSummaryRow(icon = Icons.Default.Leaderboard, label = "Skor rata-rata", value = "79%")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LegendItem(color: Color, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PerfSummaryRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ==========================================
// 8. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var reminderToggle by remember { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "Pengaturan",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section AKUN
            Column {
                Text(text = "AKUN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Akun & Langganan", fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Gratis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Bahasa Aplikasi", fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "Indonesia", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Section BELAJAR
            Column {
                Text(text = "BELAJAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Pengingat Harian", fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = reminderToggle,
                                onCheckedChange = { reminderToggle = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(icon = Icons.Default.Schedule, label = "Waktu Pengingat", value = "07:00")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsNavigationRow(icon = Icons.Default.VolumeUp, label = "Kecepatan Audio", value = "Normal")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsNavigationRow(icon = Icons.Default.TrackChanges, label = "Target Harian", value = "20 menit")
                    }
                }
            }

            // Section TAMPILAN
            val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeLabel = when (currentThemeMode) {
                "Terang" -> "Terang"
                "Gelap" -> "Gelap"
                else -> "Default Sistem"
            }

            Column {
                Text(text = "TAMPILAN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Tema Aplikasi", fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = themeLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Ukuran Teks", fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = {
                        Text(
                            text = "Pilih Tema Aplikasi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val options = listOf(
                                "Sistem" to "Default Sistem",
                                "Terang" to "Terang",
                                "Gelap" to "Gelap"
                            )
                            options.forEach { (key, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setThemeMode(key) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (currentThemeMode == key),
                                        onClick = { viewModel.setThemeMode(key) }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text("Selesai", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp)
                )
            }

            // Section LAINNYA
            Column {
                Text(text = "LAINNYA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        SettingsNavigationRow(icon = Icons.Default.Info, label = "Tentang Aplikasi", value = "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsNavigationRow(icon = Icons.Default.VerifiedUser, label = "Kebijakan Privasi", value = "")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Akun dihapus permanen.", Toast.LENGTH_LONG).show()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Hapus Akun", fontSize = 15.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Version info footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Versi 2.4.0 (Build 502)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LINGUAFLOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

// ==========================================
// CENTRAL SHARED NAVIGATION BOTTOM BAR
// ==========================================
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Pair("Kuis", "quiz_selection"),
        Pair("Riwayat", "speech_history"),
        Pair("Statistik", "statistics"),
        Pair("Profil", "profile")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = Modifier.drawBehind {
            // Draw very soft top border line
            drawLine(
                color = OutlineVariant,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2f
            )
        }
    ) {
        items.forEach { (label, screenId) ->
            val isActive = currentScreen == screenId || 
                (screenId == "statistics" && currentScreen == "statistics") ||
                (screenId == "speech_history" && currentScreen == "speech_history") ||
                (screenId == "profile" && (currentScreen == "profile" || currentScreen == "edit_profile" || currentScreen == "settings"))

            val icon = when (screenId) {
                "quiz_selection" -> Icons.Default.Quiz
                "speech_history" -> Icons.Default.History
                "statistics" -> Icons.Default.Leaderboard
                else -> Icons.Default.Person
            }

            NavigationBarItem(
                selected = isActive,
                onClick = { onNavigate(screenId) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

