package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Database(entities = [Word::class, PracticeHistory::class, SpeechHistory::class, UserProgress::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun practiceHistoryDao(): PracticeHistoryDao
    abstract fun speechHistoryDao(): SpeechHistoryDao
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linguaflow_database"
                )
                .addMigrations(MIGRATION_2_3)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.wordDao(), database.practiceHistoryDao())
                    populateSpeechHistory(database.speechHistoryDao())
                    populateUserProgress(database.userProgressDao())
                }
            }
        }

        private suspend fun populateUserProgress(dao: UserProgressDao) {
            dao.upsert(UserProgress(userId = "local"))
        }

        suspend fun populateDatabase(wordDao: WordDao, historyDao: PracticeHistoryDao) {
            val words = listOf(
                Word(
                    kanji = "食べる",
                    reading = "たべる",
                    translationIndonesian = "makan",
                    translationEnglish = "To eat / consume",
                    category = "Kata Kerja",
                    jlptLevel = "N5",
                    examplesJson = "あした、すしを食べます。_Besok, saya akan makan sushi.\nいっしょに食べましょうか。_Maukah kamu makan bersama?",
                    formsJson = "Kamus: 食べる (Taberu)\nTe-form: 食べて (Tabete)\nNai-form: 食べない (Tabenai)\nMasu-form: 食べます (Tabemasu)"
                ),
                Word(
                    kanji = "会う",
                    reading = "あう",
                    translationIndonesian = "bertemu",
                    translationEnglish = "To meet",
                    category = "Kata Kerja",
                    jlptLevel = "N5",
                    examplesJson = "ともだちに会います。_Akan bertemu teman.",
                    formsJson = "Kamus: 会う (Au)\nTe-form: 会って (Atte)\nNai-form: 会わない (Awanai)\nMasu-form: 会います (Aimasu)"
                ),
                Word(
                    kanji = "青い",
                    reading = "あおい",
                    translationIndonesian = "biru",
                    translationEnglish = "Blue adjective",
                    category = "Kata Sifat",
                    jlptLevel = "N5",
                    examplesJson = "青い空がきれいです。_Langit biru itu indah.",
                    formsJson = "Kamus: 青い (Aoi)\nNegatif: 青くない (Aokunai)\nLampau: 青かった (Aokatta)"
                ),
                Word(
                    kanji = "赤",
                    reading = "あか",
                    translationIndonesian = "merah",
                    translationEnglish = "Red",
                    category = "Kata Benda",
                    jlptLevel = "N5",
                    examplesJson = "赤いリンゴが好きです。_Saya suka apel merah.",
                    formsJson = "Kamus: 赤 (Aka)\nFormal: 赤です (Aka desu)"
                ),
                Word(
                    kanji = "明るい",
                    reading = "あかるい",
                    translationIndonesian = "terang",
                    translationEnglish = "Bright / cheerful",
                    category = "Kata Sifat",
                    jlptLevel = "N5",
                    examplesJson = "明るい部屋です。_Kamar yang terang.",
                    formsJson = "Kamus: 明るい (Akarui)\nNegatif: 明るくない (Akarukunai)\nLampau: 明るかった (Akarukatta)"
                ),
                Word(
                    kanji = "空ける",
                    reading = "あける",
                    translationIndonesian = "membuka",
                    translationEnglish = "To open / empty",
                    category = "Kata Kerja",
                    jlptLevel = "N5",
                    examplesJson = "ドアを空けます。_Membuka pintu.",
                    formsJson = "Kamus: 空ける (Akeru)\nTe-form: 空けて (Akete)\nNai-form: 空けない (Akenai)\nMasu-form: 空けます (Akemasu)"
                ),
                Word(
                    kanji = "飲む",
                    reading = "のむ",
                    translationIndonesian = "minum",
                    translationEnglish = "To drink",
                    category = "Kata Kerja",
                    jlptLevel = "N5",
                    examplesJson = "お茶を飲みます。_Minum teh hijau.",
                    formsJson = "Kamus: 飲む (Nomu)\nTe-form: 飲んで (Nonde)\nNai-form: 飲まない (Nomanai)\nMasu-form: 飲みます (Nomimasu)"
                ),
                Word(
                    kanji = "行く",
                    reading = "いく",
                    translationIndonesian = "pergi",
                    translationEnglish = "To go",
                    category = "Kata Kerja",
                    jlptLevel = "N5",
                    examplesJson = "学校へ行きます。_Pergi ke sekolah.",
                    formsJson = "Kamus: 行く (Iku)\nTe-form: 行って (Itte)\nNai-form: 行かない (Ikanai)\nMasu-form: 行きます (Ikimasu)"
                ),
                Word(
                    kanji = "来る",
                    reading = "くる",
                    translationIndonesian = "datang",
                    translationEnglish = "To come",
                    category = "Kata Kerja",
                    jlptLevel = "N5",
                    examplesJson = "友達が来ます。_Teman akan datang.",
                    formsJson = "Kamus: 来る (Kuru)\nTe-form: 来て (Kite)\nNai-form: 来ない (Konai)\nMasu-form: 来ます (Kimasu)"
                ),
                Word(
                    kanji = "本",
                    reading = "ほん",
                    translationIndonesian = "buku",
                    translationEnglish = "Book",
                    category = "Kata Benda",
                    jlptLevel = "N5",
                    examplesJson = "日本語の本を読みます。_Membaca buku bahasa Jepang.",
                    formsJson = "Kamus: 本 (Hon)\nFormal: 本です (Hon desu)"
                ),
                Word(
                    kanji = "学生",
                    reading = "がくせい",
                    translationIndonesian = "siswa",
                    translationEnglish = "Student",
                    category = "Kata Benda",
                    jlptLevel = "N5",
                    examplesJson = "私は学生です。_Saya adalah siswa.",
                    formsJson = "Kamus: 学生 (Gakusei)\nFormal: 学生です (Gakusei desu)"
                ),
                Word(
                    kanji = "寿司",
                    reading = "すし",
                    translationIndonesian = "sushi",
                    translationEnglish = "Sushi",
                    category = "Kata Benda",
                    jlptLevel = "N5",
                    examplesJson = "寿司を食べます。_Makan sushi.",
                    formsJson = "Kamus: 寿司 (Sushi)\nFormal: 寿司です (Sushi desu)"
                )
            )
            wordDao.insertAll(words)

            val histories = listOf(
                PracticeHistory(
                    title = "Kuis Kosakata: JLPT N3",
                    score = 95,
                    category = "Kuis Kosakata",
                    timestamp = System.currentTimeMillis() - 3600000 * 2
                ),
                PracticeHistory(
                    title = "Kuis Kanji: Katakana Dasar",
                    score = 88,
                    category = "Kuis Kanji",
                    timestamp = System.currentTimeMillis() - 3600000 * 24
                ),
                PracticeHistory(
                    title = "Latihan Ucapan: Perkenalan",
                    score = 72,
                    category = "Latihan Ucapan",
                    timestamp = System.currentTimeMillis() - 3600000 * 48
                )
            )
            for (h in histories) {
                historyDao.insertHistory(h)
            }
        }

        private suspend fun populateSpeechHistory(dao: SpeechHistoryDao) {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            dao.insertHistory(SpeechHistory(
                dateText = "Senin, 23 Jun 2026",
                timestamp = format.parse("2026-06-23")?.time ?: System.currentTimeMillis(),
                durationText = "0:34",
                targetSentence = "私は毎日日本語を勉強します。",
                romaji = "Watashi wa mainichi nihongo o benkyou shimasu.",
                overallScore = 82,
                clarityScore = 85,
                intonationScore = 80,
                fluencyScore = 81,
                feedback = "Pengucapan '勉強' dan '毎日' sangat lancar. Sedikit penekanan pada partikel 'を' akan membuat intonasi Anda lebih alami.",
                wordsEvaluations = "私:2,は:2,毎日:2,日本語:2,を:1,勉強:2,します:2"
            ))

            dao.insertHistory(SpeechHistory(
                dateText = "Minggu, 22 Jun 2026",
                timestamp = format.parse("2026-06-22")?.time ?: System.currentTimeMillis(),
                durationText = "0:28",
                targetSentence = "こんにちは、お元気ですか？",
                romaji = "Konnichiwa, o-genki desu ka?",
                overallScore = 74,
                clarityScore = 76,
                intonationScore = 71,
                fluencyScore = 75,
                feedback = "Pengucapan salam pembuka sudah jelas. Hati-hati dengan intonasi tanya pada akhir kalimat 'ですか'.",
                wordsEvaluations = "こんにちは:2,お元気:2,ですか:1"
            ))

            dao.insertHistory(SpeechHistory(
                dateText = "Sabtu, 21 Jun 2026",
                timestamp = format.parse("2026-06-21")?.time ?: System.currentTimeMillis(),
                durationText = "0:45",
                targetSentence = "美味しい寿司を食べたいです。",
                romaji = "Oishii sushi o tabetai desu.",
                overallScore = 88,
                clarityScore = 90,
                intonationScore = 86,
                fluencyScore = 88,
                feedback = "Luar biasa! Pengucapan kata '美味しい' dan '寿司' terdengar sangat fasih seperti penutur asli.",
                wordsEvaluations = "美味しい:2,寿司:2,を:2,食べたい:2,です:2"
            ))

            dao.insertHistory(SpeechHistory(
                dateText = "Jumat, 20 Jun 2026",
                timestamp = format.parse("2026-06-20")?.time ?: System.currentTimeMillis(),
                durationText = "0:15",
                targetSentence = "銀行はどこにありますか？",
                romaji = "Ginkou wa doko ni arimasu ka?",
                overallScore = 56,
                clarityScore = 60,
                intonationScore = 52,
                fluencyScore = 56,
                feedback = "Kata '銀行' (Ginkou) kurang jelas terdengar. Coba ucapkan 'n' di tengah kata dengan lebih jelas seperti mendengung.",
                wordsEvaluations = "銀行:0,は:1,どこに:2,ありますか:2"
            ))
        }
    }
}
