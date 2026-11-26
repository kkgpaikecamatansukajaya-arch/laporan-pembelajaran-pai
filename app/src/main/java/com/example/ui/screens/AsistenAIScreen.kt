package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.TextDark
import com.example.util.AiChatMessage
import com.example.util.GeminiAssistant

data class AiTemplate(
    val name: String,
    val docType: String,
    val topic: String,
    val classLevel: String = "IV"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenAIScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // 0 = Chat Interaktif AI, 1 = Generator Dokumen CP 020, 2 = Studio Media AI (Veo & Gambar)
    var assistantMode by remember { mutableIntStateOf(0) }

    val isGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiGeneratedResponse.collectAsStateWithLifecycle()
    val lastDocType by viewModel.lastGeneratedDocType.collectAsStateWithLifecycle()
    val lastTopic by viewModel.lastGeneratedTopic.collectAsStateWithLifecycle()

    val chatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isAiChatLoading.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customGeminiApiKey.collectAsStateWithLifecycle()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }

    // Generator Document Form States
    var selectedDocType by remember { mutableStateOf("Modul Ajar") }
    var userPrompt by remember { mutableStateOf("") }
    var classLevelSelection by remember { mutableStateOf("IV") }
    var displayedResponse by remember { mutableStateOf("") }

    // Interactive Chat Form State
    var chatInput by remember { mutableStateOf("") }
    var chatClassLevel by remember { mutableStateOf("IV") }
    val chatListState = rememberLazyListState()

    val templates = remember {
        listOf(
            AiTemplate(
                name = "Modul Ajar (CP 020)",
                docType = "Modul Ajar",
                topic = "Menyambut Usia Baligh & Bersuci (Taharah)",
                classLevel = "IV"
            ),
            AiTemplate(
                name = "Soal HOTS SOLO",
                docType = "Asesmen",
                topic = "Indahnya Saling Menghargai dalam Keragaman (Q.S. Al-Hujurat:13)",
                classLevel = "IV"
            ),
            AiTemplate(
                name = "Alur ATP (CP 020)",
                docType = "ATP",
                topic = "Zakat, Infak, dan Sedekah sebagai Wujud Cinta Sesama",
                classLevel = "V"
            ),
            AiTemplate(
                name = "KKTP & Rubrik SOLO",
                docType = "KKTP",
                topic = "Meneladani Asmaulhusna (Al-Malik, Al-Aziz, Al-Quddus, As-Salam, Al-Mu'min)",
                classLevel = "IV"
            ),
            AiTemplate(
                name = "LKPD Kelompok",
                docType = "LKPD",
                topic = "Kisah Hijrah Nabi Muhammad SAW ke Madinah",
                classLevel = "IV"
            ),
            AiTemplate(
                name = "Refleksi 5 Cinta",
                docType = "Refleksi",
                topic = "Menyayangi Anak Yatim (Q.S. Al-Ma'un) & Lima Dimensi Cinta",
                classLevel = "V"
            )
        )
    }

    val docCategories = remember {
        listOf("Modul Ajar", "ATP", "KKTP", "LKPD", "Asesmen", "Bahan Ajar", "Prota", "Promes", "Refleksi", "CP")
    }

    LaunchedEffect(aiResponse) {
        if (aiResponse.isNotBlank()) {
            displayedResponse = aiResponse
        }
    }

    LaunchedEffect(chatMessages.size, isChatLoading) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Engine Status Banner + Mode Tabs
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Asisten Pintar Guru PAI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GreenPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF16A34A))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (GeminiAssistant.hasValidApiKey()) "Gemini API Aktif" else "Siap & Aktif",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Standar: SK Kepala BSKAP No. 020 Tahun 2026 • 5 Dimensi Cinta",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            apiKeyInput = customApiKey
                            showApiKeyDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Pengaturan Kunci API",
                            modifier = Modifier.size(14.dp),
                            tint = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("API AI", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // 3-Mode Tab Row
                PrimaryTabRow(
                    selectedTabIndex = assistantMode,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GreenPrimary
                ) {
                    Tab(
                        selected = assistantMode == 0,
                        onClick = { assistantMode = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    "Chat AI PAI",
                                    fontSize = 11.sp,
                                    fontWeight = if (assistantMode == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = assistantMode == 1,
                        onClick = { assistantMode = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    "Generator CP 020",
                                    fontSize = 11.sp,
                                    fontWeight = if (assistantMode == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = assistantMode == 2,
                        onClick = { assistantMode = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.VideoCameraBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = GoldAccent
                                )
                                Text(
                                    "Studio Media",
                                    fontSize = 11.sp,
                                    fontWeight = if (assistantMode == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }
        }

        // Content based on selected tab
        when (assistantMode) {
            0 -> InteractiveChatSection(
                viewModel = viewModel,
                chatMessages = chatMessages,
                isChatLoading = isChatLoading,
                chatInput = chatInput,
                onChatInputChange = { chatInput = it },
                chatClassLevel = chatClassLevel,
                onChatClassLevelChange = { chatClassLevel = it },
                chatListState = chatListState,
                onSwitchToDocGenerator = { presetDocType, presetTopic ->
                    selectedDocType = presetDocType
                    userPrompt = presetTopic
                    classLevelSelection = chatClassLevel
                    assistantMode = 1
                    viewModel.generateDocumentWithAI(presetDocType, presetTopic, chatClassLevel)
                }
            )

            1 -> DocumentGeneratorSection(
                viewModel = viewModel,
                templates = templates,
                docCategories = docCategories,
                selectedDocType = selectedDocType,
                onSelectDocType = { selectedDocType = it },
                userPrompt = userPrompt,
                onUserPromptChange = { userPrompt = it },
                classLevelSelection = classLevelSelection,
                onClassLevelChange = { classLevelSelection = it },
                isGenerating = isGenerating,
                displayedResponse = displayedResponse,
                lastDocType = lastDocType,
                lastTopic = lastTopic,
                onClear = {
                    userPrompt = ""
                    displayedResponse = ""
                    viewModel.clearGeneratedResponse()
                }
            )

            2 -> MediaStudioScreen(viewModel = viewModel)
        }
    }

    // Dialog Konfigurasi Kunci API Gemini
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pengaturan Mesin & API Gemini AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Status Mesin AI Saat Ini:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GreenPrimary
                            )
                            Text(
                                text = if (GeminiAssistant.hasValidApiKey())
                                    "✅ Terhubung dengan Kunci API Gemini (Mode Hybrid Cloud + Mesin Kurikulum PAI CP 020/2026)"
                                else
                                    "✅ Mesin Pintar Kurikulum PAI CP 020/2026 Aktif (Berfungsi 100% tanpa perlu kuota/kunci eksternal)",
                                fontSize = 11.sp,
                                color = TextDark
                            )
                        }
                    }

                    Text(
                        text = "Opsional: Masukkan Kunci API Google Gemini (AI Studio) Anda jika ingin menggunakan endpoint cloud langsung (gemini-3.5-flash, gemini-3.1-pro-preview, gemini-3.1-flash-image-preview, & veo-3.1-fast-generate-preview):",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key (Opsional)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomGeminiApiKey(apiKeyInput)
                        showApiKeyDialog = false
                        Toast.makeText(context, "Konfigurasi Asisten AI disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractiveChatSection(
    viewModel: AppViewModel,
    chatMessages: List<AiChatMessage>,
    isChatLoading: Boolean,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    chatClassLevel: String,
    onChatClassLevelChange: (String) -> Unit,
    chatListState: androidx.compose.foundation.lazy.LazyListState,
    onSwitchToDocGenerator: (String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val quickQuestions = remember {
        listOf(
            "Buatkan Modul Ajar Menyambut Usia Baligh",
            "Buatkan 5 Soal HOTS Q.S. Al-Hujurat:13",
            "Metode mengajar tajwid agar siswa antusias",
            "Buatkan LKPD Kisah Hijrah Nabi SAW",
            "Ide ice breaking Islami sebelum belajar PAI",
            "Contoh rubrik penilaian Taksonomi SOLO"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top Bar: Class selector + Clear chat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text("Kelas:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                listOf("I", "II", "III", "IV", "V", "VI").forEach { lvl ->
                    FilterChip(
                        selected = chatClassLevel == lvl,
                        onClick = { onChatClassLevelChange(lvl) },
                        label = { Text(lvl, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            TextButton(
                onClick = {
                    viewModel.clearAiChatHistory()
                    Toast.makeText(context, "Percakapan direset", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Chat", fontSize = 11.sp, color = Color.Gray)
            }
        }

        // Quick Prompt Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickQuestions.forEach { q ->
                SuggestionChip(
                    onClick = {
                        viewModel.sendAiChatMessage(q, chatClassLevel)
                    },
                    label = { Text(q, fontSize = 11.sp) },
                    icon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = GreenPrimary
                    )
                )
            }
        }

        // Messages List
        LazyColumn(
            state = chatListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val isUser = msg.isFromUser
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 340.dp),
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isUser) 14.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 14.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) GreenPrimary else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "Anda (Guru PAI • Kelas ${msg.classLevelHint})" else msg.modelSource,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) GoldAccent else GreenPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            SelectionContainer {
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = if (isUser) Color.White else TextDark
                                )
                            }

                            // Action Bar for AI Responses
                            if (!isUser) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(msg.text))
                                            Toast.makeText(context, "Teks disalin ke papan klip", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Salin", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val docType = msg.docTypeHint.ifBlank { "Modul Ajar" }
                                            val title = "$docType PAI Kelas ${msg.classLevelHint} - Hasil Asisten AI"
                                            viewModel.addDocument(
                                                type = docType,
                                                title = title,
                                                classLevel = msg.classLevelHint,
                                                contentText = msg.text
                                            )
                                            Toast.makeText(context, "Disimpan ke Perangkat Pembelajaran ($docType)", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Simpan ke Perangkat", fontSize = 10.sp, color = Color.White)
                                    }

                                    if (msg.text.contains("Kunci Jawaban:", ignoreCase = true)) {
                                        Button(
                                            onClick = {
                                                viewModel.saveGeneratedQuestionsToBank(msg.text, "HOTS PAI Kelas ${msg.classLevelHint}") { count ->
                                                    Toast.makeText(context, "$count Soal berhasil ditambahkan ke Bank Soal PAI!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                        ) {
                                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Bank Soal", fontSize = 10.sp, color = Color.White)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val docType = msg.docTypeHint.ifBlank { "Modul Ajar" }
                                            onSwitchToDocGenerator(docType, "Pengembangan Materi PAI Kelas ${msg.classLevelHint}")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Buka Generator", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenPrimary, strokeWidth = 2.dp)
                            Text("Asisten AI sedang menyusun jawaban PAI...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Chat Input Row
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = onChatInputChange,
                    placeholder = { Text("Tulis pertanyaan atau minta buatkan modul, soal, LKPD...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilledIconButton(
                    onClick = {
                        if (chatInput.isBlank()) {
                            Toast.makeText(context, "Ketik pesan terlebih dahulu", Toast.LENGTH_SHORT).show()
                        } else {
                            val toSend = chatInput
                            onChatInputChange("")
                            viewModel.sendAiChatMessage(toSend, chatClassLevel)
                        }
                    },
                    enabled = !isChatLoading,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = GreenPrimary),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("ai_chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Pesan AI", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentGeneratorSection(
    viewModel: AppViewModel,
    templates: List<AiTemplate>,
    docCategories: List<String>,
    selectedDocType: String,
    onSelectDocType: (String) -> Unit,
    userPrompt: String,
    onUserPromptChange: (String) -> Unit,
    classLevelSelection: String,
    onClassLevelChange: (String) -> Unit,
    isGenerating: Boolean,
    displayedResponse: String,
    lastDocType: String,
    lastTopic: String,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Templates Quick Selector (One-Tap Fill & Generate Ready)
        Column {
            Text(
                text = "Template Cepat Perangkat PAI (SK BSKAP 020/2026):",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { temp ->
                    OutlinedButton(
                        onClick = {
                            onSelectDocType(temp.docType)
                            onUserPromptChange(temp.topic)
                            onClassLevelChange(temp.classLevel)
                            viewModel.generateDocumentWithAI(temp.docType, temp.topic, temp.classLevel)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(temp.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. Generator Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pilih Jenis Dokumen Kurikulum Merdeka:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    docCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedDocType == cat,
                            onClick = { onSelectDocType(cat) },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Class Level Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tingkat Kelas:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    listOf("I", "II", "III", "IV", "V", "VI").forEach { lvl ->
                        val isSelected = classLevelSelection == lvl
                        FilterChip(
                            selected = isSelected,
                            onClick = { onClassLevelChange(lvl) },
                            label = { Text(lvl, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = onUserPromptChange,
                    placeholder = {
                        Text("Masukkan topik/materi pokok PAI, misal: Menyambut Usia Baligh, Q.S. Al-Hujurat:13, Asmaulhusna, Zakat Fitrah, atau Kisah Hijrah Nabi SAW...")
                    },
                    label = { Text("Topik / Materi Pokok untuk $selectedDocType") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .testTag("ai_prompt_input"),
                    singleLine = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Reset", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (userPrompt.isBlank()) {
                                Toast.makeText(context, "Silakan isi topik/materi terlebih dahulu", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.generateDocumentWithAI(selectedDocType, userPrompt, classLevelSelection)
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("ai_send_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Menyusun $selectedDocType...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Susun $selectedDocType", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Output Response Display Section
        if (isGenerating || displayedResponse.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hasil Penyusunan: $lastDocType (Kelas $classLevelSelection)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "CP BSKAP 020/2026",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isGenerating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = GreenPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Asisten AI sedang memformulasikan $selectedDocType PAI terbaik...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        SelectionContainer {
                            Text(
                                text = displayedResponse,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(displayedResponse))
                                    Toast.makeText(context, "Teks disalin ke papan klip", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                                border = BorderStroke(1.dp, GreenPrimary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "$lastDocType PAI Kelas $classLevelSelection - $lastTopic")
                                        putExtra(Intent.EXTRA_TEXT, displayedResponse)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Dokumen PAI"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                                border = BorderStroke(1.dp, GreenPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bagikan", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val cleanTopicTitle = lastTopic.ifBlank { userPrompt.ifBlank { "Kurikulum Merdeka PAI" } }.take(60)
                                    val docTypeToSave = lastDocType.ifBlank { selectedDocType }
                                    viewModel.addDocument(
                                        type = docTypeToSave,
                                        title = "$docTypeToSave - $cleanTopicTitle (Kelas $classLevelSelection)",
                                        classLevel = classLevelSelection,
                                        contentText = displayedResponse
                                    )
                                    Toast.makeText(
                                        context,
                                        "Berhasil disimpan di Perangkat Pembelajaran ($docTypeToSave)!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan ke Perangkat", fontSize = 12.sp, color = Color.White)
                            }

                            if (lastDocType == "Asesmen" || displayedResponse.contains("Kunci Jawaban:", ignoreCase = true)) {
                                Button(
                                    onClick = {
                                        viewModel.saveGeneratedQuestionsToBank(
                                            rawText = displayedResponse,
                                            topic = lastTopic.ifBlank { userPrompt }
                                        ) { count ->
                                            if (count > 0) {
                                                Toast.makeText(context, "$count Soal HOTS berhasil dimasukkan ke Bank Soal PAI!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Soal disimpan ke Perangkat Asesmen", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) {
                                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Masukkan ke Bank Soal", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
