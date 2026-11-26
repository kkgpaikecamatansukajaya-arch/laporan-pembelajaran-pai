package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HafalanSuratScreen(
    viewModel: AppViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val hafalanRecords by viewModel.allHafalanRecords.collectAsStateWithLifecycle()
    val school by viewModel.schoolData.collectAsStateWithLifecycle()
    val teacher by viewModel.teacherProfile.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Katalog Surat Juz 'Amma, 1: Mutaba'ah & Setoran Siswa
    var selectedFaseFilter by remember { mutableStateOf<String?>(null) } // "Semua", "Fase A", "Fase B", "Fase C"
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for adding / testing memorization
    var showAddSetoranDialog by remember { mutableStateOf(false) }
    var preselectedSurahForDialog by remember { mutableStateOf<SurahItem?>(null) }
    var selectedSurahForReading by remember { mutableStateOf<SurahItem?>(null) }

    LaunchedEffect(classes) {
        if (selectedClassId == null && classes.isNotEmpty()) {
            selectedClassId = classes.first().id
        }
    }

    val surahs = viewModel.surahCatalog

    val filteredSurahs = remember(surahs, selectedFaseFilter, searchQuery) {
        surahs.filter { item ->
            val matchFase = selectedFaseFilter == null || item.fase.startsWith(selectedFaseFilter!!)
            val matchQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.meaning.contains(searchQuery, ignoreCase = true) ||
                item.arabicName.contains(searchQuery)
            matchFase && matchQuery
        }
    }

    val filteredRecords = remember(hafalanRecords, selectedClassId, searchQuery) {
        hafalanRecords.filter { r ->
            val matchClass = selectedClassId == null || r.classId == selectedClassId
            val matchQuery = searchQuery.isBlank() ||
                r.studentName.contains(searchQuery, ignoreCase = true) ||
                r.surahName.contains(searchQuery, ignoreCase = true)
            matchClass && matchQuery
        }
    }

    val totalRecords = filteredRecords.size
    val lulusRecords = filteredRecords.count { it.status != HafalanStatus.PERLU_MURAJAAH && it.status != HafalanStatus.BELUM_SETOR }
    val lulusPct = if (totalRecords > 0) (lulusRecords * 100) / totalRecords else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hafalan Surat Pendek",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Juz 'Amma • Mutaba'ah Tahfidz Kurikulum Merdeka",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val activeClassName = classes.find { it.id == selectedClassId }?.name ?: "Semua Rombel"
                        val sampleText = viewModel.formatHafalanShareText(
                            studentName = "Rekap Siswa $activeClassName",
                            className = activeClassName,
                            records = filteredRecords
                        )
                        clipboardManager.setText(AnnotatedString(sampleText))
                        Toast.makeText(context, "Laporan mutaba'ah hafalan berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = GoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    preselectedSurahForDialog = null
                    showAddSetoranDialog = true
                },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Setor Hafalan")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Banner: Mutaba'ah Tahfidz PAI SD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hafalan_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSecondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text(
                                            text = "Tahfidz & Tilawah Al-Qur'an",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Fase A (Kls 1-2) • Fase B (Kls 3-4) • Fase C (Kls 5-6)",
                                            fontSize = 10.sp,
                                            color = GoldLight
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${surahs.size} Surat",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "TOTAL SETORAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text(text = "$totalRecords Anak", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text(text = "TUNTAS / LULUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text(text = "$lulusRecords Anak ($lulusPct%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                                }
                                Column {
                                    Text(text = "GURU PENGUJI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                    Text(text = teacher.fullName.take(18), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Sub-Tab Switcher: [0: Katalog Surat Juz 'Amma] vs [1: Setoran & Mutaba'ah Siswa]
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeTab = 0 },
                            color = if (activeTab == 0) GreenPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (activeTab == 0) GoldLight else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Katalog Surat Pendek",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeTab = 1 },
                            color = if (activeTab == 1) GreenPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) GoldLight else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Setoran Siswa (${hafalanRecords.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (activeTab == 0) "Cari nama surat (contoh: An-Nas, Al-Falaq)..."
                                else "Cari siswa atau surat...",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    if (activeTab == 0) {
                        // Filter Fase Kurikulum Merdeka
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedFaseFilter == null,
                                onClick = { selectedFaseFilter = null },
                                label = { Text("Semua Target", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
                            )
                            listOf("Fase A", "Fase B", "Fase C").forEach { fase ->
                                val isSelected = selectedFaseFilter == fase
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFaseFilter = fase },
                                    label = { Text(fase, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    } else {
                        // Filter Kelas Rombel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedClassId == null,
                                onClick = { selectedClassId = null },
                                label = { Text("Semua Kelas", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
                            )
                            classes.forEach { cls ->
                                val isSelected = selectedClassId == cls.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedClassId = cls.id },
                                    label = { Text(cls.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // TAB 0: KATALOG SURAT JUZ 'AMMA
            if (activeTab == 0) {
                item {
                    Text(
                        text = "Katalog Surat Pendek (${filteredSurahs.size} Surat)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }

                items(filteredSurahs, key = { it.number }) { surah ->
                    SurahCard(
                        surah = surah,
                        onOpenReading = { selectedSurahForReading = surah },
                        onSimakSetoran = {
                            preselectedSurahForDialog = surah
                            showAddSetoranDialog = true
                        }
                    )
                }
            } else {
                // TAB 1: SETORAN SISWA & MUTABA'AH
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Catatan Mutaba'ah Siswa (${filteredRecords.size} Setoran)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        TextButton(
                            onClick = {
                                preselectedSurahForDialog = null
                                showAddSetoranDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Setor Nilai", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (filteredRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Belum ada catatan setoran hafalan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Text("Klik tombol 'Setor Nilai' untuk mencatat setoran siswa.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(filteredRecords, key = { it.id }) { record ->
                        HafalanRecordCard(
                            record = record,
                            onDelete = {
                                viewModel.deleteHafalanRecord(record.id)
                                Toast.makeText(context, "Data setoran dihapus.", Toast.LENGTH_SHORT).show()
                            },
                            onShareStudent = {
                                val text = viewModel.formatHafalanShareText(
                                    studentName = record.studentName,
                                    className = record.className,
                                    records = listOf(record)
                                )
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Mutaba'ah ${record.studentName} disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Modal Sheet Detail Bacaan Surat & Transliterasi
    selectedSurahForReading?.let { readingSurah ->
        AlertDialog(
            onDismissRequest = { selectedSurahForReading = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Q.S. ${readingSurah.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GreenPrimary
                        )
                        Text(
                            text = "${readingSurah.meaning} • ${readingSurah.versesCount} Ayat (${readingSurah.revelation})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = readingSurah.arabicName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Teks Arab
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Lafadz Arab:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = readingSurah.arabicSnippet,
                                    fontSize = 18.sp,
                                    lineHeight = 32.sp,
                                    textAlign = TextAlign.Right,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Transliterasi Latin
                    item {
                        Column {
                            Text(text = "Transliterasi Latin:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = readingSurah.latinSnippet, fontSize = 12.sp, color = TextDark, lineHeight = 18.sp)
                        }
                    }

                    // Terjemahan
                    item {
                        Column {
                            Text(text = "Arti Bahasa Indonesia:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = readingSurah.translationSnippet, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 17.sp)
                        }
                    }

                    // Tajwid & Hikmah
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Fokus Tajwid: ${readingSurah.tajwidFocus}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF78350F))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Kandungan: ${readingSurah.wisdom}", fontSize = 10.sp, color = Color(0xFF92400E))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toTest = readingSurah
                        selectedSurahForReading = null
                        preselectedSurahForDialog = toTest
                        showAddSetoranDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uji Siswa Sekarang", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSurahForReading = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Modal Input Setoran Baru
    if (showAddSetoranDialog) {
        AddSetoranDialog(
            classes = classes,
            students = allStudents,
            surahs = surahs,
            initialSurah = preselectedSurahForDialog,
            teacherName = teacher.fullName,
            onDismiss = { showAddSetoranDialog = false },
            onSave = { record ->
                viewModel.addOrUpdateHafalanRecord(record)
                showAddSetoranDialog = false
                Toast.makeText(context, "Setoran ${record.surahName} untuk ${record.studentName} berhasil dicatat!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SurahCard(
    surah: SurahItem,
    onOpenReading: () -> Unit,
    onSimakSetoran: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_card_${surah.number}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${surah.number}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GreenPrimary
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Q.S. ${surah.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    surah.fase.contains("Fase A") -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    surah.fase.contains("Fase B") -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                    else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = surah.fase.take(6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        surah.fase.contains("Fase A") -> Color(0xFF047857)
                                        surah.fase.contains("Fase B") -> Color(0xFF0369A1)
                                        else -> Color(0xFFB45309)
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${surah.meaning} • ${surah.versesCount} Ayat (${surah.revelation})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = surah.arabicName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Baca Teks & Simak Setoran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenReading,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = GreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Teks & Terjemah", fontSize = 11.sp, color = GreenPrimary)
                }

                Button(
                    onClick = onSimakSetoran,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simak Setoran", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HafalanRecordCard(
    record: HafalanRecord,
    onDelete: () -> Unit,
    onShareStudent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hafalan_record_${record.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(record.status.badgeColorHex).copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.studentName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Text(
                        text = "${record.className} • ${record.date}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(record.status.badgeColorHex)
                ) {
                    Text(
                        text = "Nilai: ${record.score}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Surat: Q.S. ${record.surahName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GreenPrimary
                        )
                        Text(
                            text = record.status.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(record.status.badgeColorHex)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kelancaran: ${record.kelancaran} | Tajwid: ${record.tajwid} | Makhraj: ${record.makhraj}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    if (record.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Catatan: \"${record.notes}\"",
                            fontSize = 10.sp,
                            color = Color(0xFF0369A1),
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShareStudent, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Salin Mutaba'ah", tint = GoldAccent, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSetoranDialog(
    classes: List<ClassRoom>,
    students: List<Student>,
    surahs: List<SurahItem>,
    initialSurah: SurahItem?,
    teacherName: String,
    onDismiss: () -> Unit,
    onSave: (HafalanRecord) -> Unit
) {
    var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.id ?: 1) }
    val classStudents = remember(selectedClassId, students) {
        students.filter { it.classId == selectedClassId }
    }
    var selectedStudentId by remember { mutableStateOf(classStudents.firstOrNull()?.id ?: 0) }

    // Ensure valid selected student
    LaunchedEffect(classStudents) {
        if (classStudents.none { it.id == selectedStudentId }) {
            selectedStudentId = classStudents.firstOrNull()?.id ?: 0
        }
    }

    var selectedSurahNumber by remember { mutableStateOf(initialSurah?.number ?: 1) }
    var score by remember { mutableStateOf(90f) }
    var kelancaran by remember { mutableStateOf("Sangat Lancar") }
    var tajwid by remember { mutableStateOf("Sangat Baik") }
    var makhraj by remember { mutableStateOf("Fasih") }
    var notes by remember { mutableStateOf("") }

    val activeClass = classes.find { it.id == selectedClassId }
    val activeStudent = classStudents.find { it.id == selectedStudentId }
    val activeSurah = surahs.find { it.number == selectedSurahNumber } ?: surahs.first()

    val calculatedStatus = when {
        score >= 93 -> HafalanStatus.MUMTAZ
        score >= 85 -> HafalanStatus.JAYYID_JIDDAN
        score >= 75 -> HafalanStatus.JAYYID
        score >= 65 -> HafalanStatus.MAQBUL
        else -> HafalanStatus.PERLU_MURAJAAH
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Simak Setoran Hafalan Siswa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenPrimary)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pilih Kelas
                item {
                    Text("Pilih Kelas (Rombel):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        classes.forEach { cls ->
                            FilterChip(
                                selected = selectedClassId == cls.id,
                                onClick = { selectedClassId = cls.id },
                                label = { Text(cls.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                // Pilih Siswa
                item {
                    Text("Pilih Siswa:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (classStudents.isEmpty()) {
                        Text("Tidak ada siswa di kelas ini.", fontSize = 11.sp, color = Color.Red)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            classStudents.forEach { s ->
                                FilterChip(
                                    selected = selectedStudentId == s.id,
                                    onClick = { selectedStudentId = s.id },
                                    label = { Text(s.name, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                }

                // Pilih Surat
                item {
                    Text("Pilih Surat Pendek:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        surahs.forEach { s ->
                            FilterChip(
                                selected = selectedSurahNumber == s.number,
                                onClick = { selectedSurahNumber = s.number },
                                label = { Text(s.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenSecondary, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                // Nilai Angka Slider (0 - 100)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nilai Setoran:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${score.toInt()} (${calculatedStatus.label.take(15)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(calculatedStatus.badgeColorHex)
                            )
                        }
                        Slider(
                            value = score,
                            onValueChange = { score = it },
                            valueRange = 50f..100f,
                            steps = 10,
                            colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GreenPrimary)
                        )
                    }
                }

                // Kelancaran
                item {
                    Text("Kelancaran:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Sangat Lancar", "Lancar", "Cukup", "Kurang").forEach { level ->
                            FilterChip(
                                selected = kelancaran == level,
                                onClick = { kelancaran = level },
                                label = { Text(level, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Tajwid
                item {
                    Text("Ketepatan Tajwid:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Sangat Baik", "Baik", "Cukup", "Perlu Bimbingan").forEach { level ->
                            FilterChip(
                                selected = tajwid == level,
                                onClick = { tajwid = level },
                                label = { Text(level, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Makhraj
                item {
                    Text("Makhorijul Huruf:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Fasih", "Cukup", "Perlu Pembiasaan").forEach { level ->
                            FilterChip(
                                selected = makhraj == level,
                                onClick = { makhraj = level },
                                label = { Text(level, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Catatan Evaluasi
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan / Nasihat Guru", fontSize = 11.sp) },
                        placeholder = { Text("Contoh: Dengung nun bertasydid sudah bagus, perhatikan makhraj 'ain.", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (activeStudent != null) {
                        val record = HafalanRecord(
                            studentId = activeStudent.id,
                            studentName = activeStudent.name,
                            classId = selectedClassId,
                            className = activeClass?.name ?: "Kelas PAI",
                            surahNumber = activeSurah.number,
                            surahName = activeSurah.name,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            score = score.toInt(),
                            status = calculatedStatus,
                            kelancaran = kelancaran,
                            tajwid = tajwid,
                            makhraj = makhraj,
                            testerTeacher = teacherName,
                            notes = notes.ifBlank { "Hafalan disimak dengan baik dan tertib." }
                        )
                        onSave(record)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                enabled = activeStudent != null
            ) {
                Text("Simpan Setoran", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
