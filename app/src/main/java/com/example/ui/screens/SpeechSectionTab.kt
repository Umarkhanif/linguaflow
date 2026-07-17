package com.example.ui.screens

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.EvaluationResult
import com.example.data.SpeechHistory
import com.example.ui.MainViewModel
import com.example.ui.theme.SoftLavenderBg
import java.util.Locale

// ponytail: minimal TTS wrapper kept local; only Japanese playback needed.
private class TtsWrapper(context: Context) {
    private val tts = TextToSpeech(context) { }
    fun speak(text: String) {
        tts.language = Locale.JAPANESE
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    fun shutdown() = tts.shutdown()
}

@Composable
fun SpeechSectionTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf("practice") }
    val tts = remember { TtsWrapper(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    when (screen) {
        "practice" -> SpeechPracticeScreen(
            viewModel = viewModel,
            tts = tts,
            onShowHistory = { screen = "history" }
        )
        "history" -> SpeechHistoryList(
            viewModel = viewModel,
            onBack = { screen = "practice" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeechPracticeScreen(
    viewModel: MainViewModel,
    tts: TtsWrapper,
    onShowHistory: () -> Unit
) {
    val exercise by viewModel.currentExercise.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recognized by viewModel.recognizedText.collectAsState()
    val isEvaluating by viewModel.isEvaluating.collectAsState()
    val result by viewModel.evaluationResult.collectAsState()
    val error by viewModel.speechError.collectAsState()
    val history by viewModel.speechHistory.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Latihan Ucapan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftLavenderBg),
                actions = {
                    TextButton(onClick = onShowHistory) { Text("Riwayat") }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target sentence card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftLavenderBg)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(exercise.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(exercise.romaji, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { tts.speak(exercise.text) }) { Text("🔊 Dengarkan") }
                }
            }

            if (result != null) {
                EvaluationCard(result!!)
                Button(
                    onClick = { viewModel.nextExercise() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Latihan Berikutnya") }
            } else {
                OutlinedTextField(
                    value = recognized,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Yang kamu ucapkan") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                if (isEvaluating) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    Text("Menilai ucapanmu...", Modifier.align(Alignment.CenterHorizontally))
                }

                Button(
                    onClick = {
                        if (isRecording) viewModel.toggleRecording(context)
                        else viewModel.submitForEvaluation(context) {}
                    },
                    enabled = !isEvaluating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRecording) "Berhenti" else "Mulai Merekam")
                }

                if (!isRecording && recognized.isNotEmpty() && result == null) {
                    OutlinedButton(
                        onClick = { viewModel.submitForEvaluation(context) {} },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Nilai Ucapan") }
                }
            }

            if (history.isNotEmpty()) {
                Text("Riwayat terakhir", fontWeight = FontWeight.Bold)
                history.take(3).forEach {
                    HistoryRow(it) { tts.speak(it.targetSentence) }
                }
            }
        }
    }
}

@Composable
private fun EvaluationCard(result: EvaluationResult) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hasil Evaluasi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreChip("Keseluruhan", result.overallScore)
                ScoreChip("Kejelasan", result.clarityScore)
                ScoreChip("Intonasi", result.intonationScore)
                ScoreChip("Kelancaran", result.fluencyScore)
            }
            Text(result.feedback, fontSize = 14.sp)
            result.wordEvaluations.forEach {
                val color = when (it.score) {
                    2 -> Color(0xFF2E7D32)
                    1 -> Color(0xFFF9A825)
                    else -> Color(0xFFC62828)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.word, fontSize = 16.sp)
                    Text(
                        when (it.score) {
                            2 -> "Sempurna"
                            1 -> "Cukup"
                            else -> "Perlu perbaikan"
                        },
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SoftLavenderBg,
            modifier = Modifier.size(56.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("$value", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun HistoryRow(item: SpeechHistory, onSpeak: () -> Unit) {
    Card(
        onClick = onSpeak,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.targetSentence, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${item.dateText} • ${item.durationText}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${item.overallScore}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeechHistoryList(viewModel: MainViewModel, onBack: () -> Unit) {
    val history by viewModel.speechHistory.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsWrapper(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Ucapan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftLavenderBg)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Belum ada riwayat latihan ucapan.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { HistoryRow(it) { tts.speak(it.targetSentence) } }
            }
        }
    }
}
