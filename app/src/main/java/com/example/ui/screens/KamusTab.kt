package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Word
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KamusTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onWordClick: ((word: Word) -> Unit)? = null
) {
    val words by viewModel.allWords.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedIndexTab by remember { mutableStateOf("A") }

    val latinIndex = listOf("A", "B", "C", "D", "E")
    val kanaIndex = listOf("あ", "か", "さ", "た", "な", "は", "ま")

    // Filter words based on search query and alphabet index
    val filteredWords = remember(words, searchQuery, selectedIndexTab) {
        words.filter { word ->
            // Search filter
            val matchesQuery = word.kanji.contains(searchQuery, ignoreCase = true) ||
                    word.reading.contains(searchQuery, ignoreCase = true) ||
                    word.translationIndonesian.contains(searchQuery, ignoreCase = true) ||
                    word.translationEnglish.contains(searchQuery, ignoreCase = true)

            // Index filter
            val firstChar = word.reading.firstOrNull()?.toString() ?: ""
            val matchesIndex = if (selectedIndexTab.isEmpty()) {
                true
            } else {
                // If it's a Latin index (A, B, C, D), check translation first letter
                if (selectedIndexTab in latinIndex) {
                    word.translationIndonesian.startsWith(selectedIndexTab, ignoreCase = true)
                } else {
                    // Kana index filter
                    when (selectedIndexTab) {
                        "あ" -> firstChar in listOf("あ", "い", "う", "え", "お")
                        "か" -> firstChar in listOf("か", "き", "く", "け", "こ", "が", "ぎ", "ぐ", "げ", "ご")
                        "さ" -> firstChar in listOf("さ", "し", "す", "せ", "そ", "ざ", "じ", "ず", "ぜ", "ぞ")
                        "た" -> firstChar in listOf("た", "ち", "つ", "て", "と", "だ", "ぢ", "づ", "で", "ど")
                        "な" -> firstChar in listOf("な", "に", "ぬ", "ね", "の")
                        "は" -> firstChar in listOf("は", "ひ", "ふ", "へ", "ほ", "ば", "び", "ぶ", "べ", "ぼ")
                        "ま" -> firstChar in listOf("ま", "み", "む", "め", "も")
                        else -> true
                    }
                }
            }

            matchesQuery && matchesIndex
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search bar
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari kata atau arti...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = Color(0xFF7A7583)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFCAC4D0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Horizontal index list
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(width = 0.5.dp, color = Color(0xFFCAC4D0))
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(latinIndex) { char ->
                val isSelected = selectedIndexTab == char
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedIndexTab = char }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = char,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp))
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color(0xFFCAC4D0))
                )
            }

            items(kanaIndex) { char ->
                val isSelected = selectedIndexTab == char
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedIndexTab = char }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = char,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp))
                        )
                    }
                }
            }
        }

        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = selectedIndexTab,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20)
            )

            Text(
                text = "${filteredWords.size} kata",
                fontSize = 13.sp,
                color = Color(0xFF49454F)
            )
        }

        // Vocabulary List
        if (filteredWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada kata yang cocok.",
                    fontSize = 14.sp,
                    color = Color(0xFF494552).copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredWords) { word ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (onWordClick != null) {
                                    onWordClick(word)
                                } else {
                                    viewModel.selectWordForDetail(word)
                                }
                            }
                            .background(Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = word.kanji,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                                Text(
                                    text = word.reading,
                                    fontSize = 13.sp,
                                    color = Color(0xFF49454F)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = word.translationIndonesian,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = word.jlptLevel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.toggleBookmark(word) }
                                ) {
                                    Icon(
                                        imageVector = if (word.bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark button",
                                        tint = if (word.bookmarked) MaterialTheme.colorScheme.primary else Color(0xFFCAC4D0)
                                    )
                                }
                            }
                        }

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color(0xFFCAC4D0))
                        )
                    }
                }
            }
        }
    }
}
