package com.example.ui.screens

import android.app.DatePickerDialog
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
fun AbsensiScreen(
    viewModel: AppViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.allAttendanceRecords.collectAsStateWithLifecycle()
    val school by viewModel.schoolData.collectAsStateWithLifecycle()

    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var selectedSubject by remember { mutableStateOf("PAI & Budi Pekerti") }
    var searchQuery by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // List of subjects / activities
    val subjects = listOf(
        "PAI & Budi Pekerti",
        "Shalat Dhuha & Berjamaah",
        "Tadarus & BTQ",
        "Ekstrakurikuler Rohis"
    )

    // Ensure initial class selection
    LaunchedEffect(classes) {
        if (selectedClassId == null && classes.isNotEmpty()) {
            selectedClassId = classes.first().id
        }
    }

    val selectedClass = classes.find { it.id == selectedClassId }
    val currentStudents = remember(allStudents, selectedClassId) {
        if (selectedClassId != null) {
            allStudents.filter { it.classId == selectedClassId }
        } else {
            emptyList()
        }
    }

    // Local mutable state for current attendance session (studentId -> Entry)
    val attendanceState = remember { mutableStateMapOf<Int, AttendanceStudentEntry>() }

    // Sync local state whenever date, class, or records change
    LaunchedEffect(selectedDate, selectedClassId, selectedSubject, currentStudents, attendanceRecords) {
        attendanceState.clear()
        val existing = attendanceRecords.filter {
            it.date == selectedDate && it.classId == selectedClassId && it.subject == selectedSubject
        }.associateBy { it.studentId }

        currentStudents.forEach { student ->
            val rec = existing[student.id]
            attendanceState[student.id] = AttendanceStudentEntry(
                studentId = student.id,
                studentName = student.name,
                gender = student.gender,
                status = rec?.status ?: AttendanceStatus.HADIR,
                notes = rec?.notes ?: ""
            )
        }
    }

    val filteredEntries = remember(attendanceState.values.toList(), searchQuery) {
        val list = attendanceState.values.toList()
        if (searchQuery.isBlank()) list
        else list.filter { it.studentName.contains(searchQuery, ignoreCase = true) }
    }

    // Calculations
    val totalCount = attendanceState.size
    val hadirCount = attendanceState.values.count { it.status == AttendanceStatus.HADIR }
    val sakitCount = attendanceState.values.count { it.status == AttendanceStatus.SAKIT }
    val izinCount = attendanceState.values.count { it.status == AttendanceStatus.IZIN }
    val alpaCount = attendanceState.values.count { it.status == AttendanceStatus.ALPA }
    val attendancePct = if (totalCount > 0) (hadirCount * 100) / totalCount else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Presensi Siswa PAI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "${school.schoolName} • TA ${school.academicYear}",
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
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat Presensi", tint = GreenPrimary)
                    }
                    IconButton(onClick = {
                        val recordsToExport = attendanceState.values.map { entry ->
                            AttendanceRecord(
                                date = selectedDate,
                                classId = selectedClassId ?: 0,
                                className = selectedClass?.name ?: "Semua Kelas",
                                studentId = entry.studentId,
                                studentName = entry.studentName,
                                gender = entry.gender,
                                status = entry.status,
                                notes = entry.notes,
                                subject = selectedSubject
                            )
                        }
                        val shareText = viewModel.formatAttendanceShareText(
                            date = selectedDate,
                            className = selectedClass?.name ?: "Kelas PAI",
                            subject = selectedSubject,
                            records = recordsToExport
                        )
                        clipboardManager.setText(AnnotatedString(shareText))
                        Toast.makeText(context, "Rekap presensi berhasil disalin ke clipboard! Siap dikirim ke WhatsApp.", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Rekap", tint = GoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
            // 1. Header Filter: Tanggal & Mapel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
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
                            // Date Picker trigger
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GreenPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.clickable {
                                    val calendar = Calendar.getInstance()
                                    try {
                                        val parts = selectedDate.split("-")
                                        if (parts.size == 3) {
                                            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                        }
                                    } catch (_: Exception) {}
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = selectedDate,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Quick Day Navigators
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Hari Ini", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subject Selection
                        Text(
                            text = "Kegiatan / Mata Pelajaran:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subjects.forEach { subj ->
                                val isSelected = selectedSubject == subj
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Class Selection Chips
                        Text(
                            text = "Pilih Rombongan Belajar (Kelas):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            classes.forEach { cls ->
                                val isSelected = selectedClassId == cls.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedClassId = cls.id },
                                    label = { Text(cls.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Summary & Stats Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_stats_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSecondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rekap Kehadiran: ${selectedClass?.name ?: "-"}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$selectedSubject • $selectedDate",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldAccent
                            ) {
                                Text(
                                    text = "$attendancePct% Hadir",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats counters row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AttendanceCountBadge(title = "Hadir", count = hadirCount, color = Color(0xFF10B981))
                            AttendanceCountBadge(title = "Sakit", count = sakitCount, color = Color(0xFFF59E0B))
                            AttendanceCountBadge(title = "Izin", count = izinCount, color = Color(0xFF38BDF8))
                            AttendanceCountBadge(title = "Alpa", count = alpaCount, color = Color(0xFFEF4444))
                            AttendanceCountBadge(title = "Total", count = totalCount, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) hadirCount.toFloat() / totalCount else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = GoldLight,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            // 3. Quick Action Bar: "Hadir Semua", "Cari Siswa", "Simpan"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // One-tap Mark All Present Button
                    Button(
                        onClick = {
                            currentStudents.forEach { s ->
                                val old = attendanceState[s.id]
                                attendanceState[s.id] = AttendanceStudentEntry(
                                    studentId = s.id,
                                    studentName = s.name,
                                    gender = s.gender,
                                    status = AttendanceStatus.HADIR,
                                    notes = old?.notes ?: ""
                                )
                            }
                            Toast.makeText(context, "Seluruh siswa ditandai Hadir!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hadir Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            if (selectedClassId != null && selectedClass != null) {
                                viewModel.saveAttendanceBatch(
                                    date = selectedDate,
                                    classId = selectedClassId!!,
                                    className = selectedClass.name,
                                    subject = selectedSubject,
                                    entries = attendanceState.values.toList()
                                )
                                Toast.makeText(context, "Presensi ${selectedClass.name} berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama siswa...", fontSize = 12.sp) },
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
            }

            // 4. Student List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Siswa (${filteredEntries.size} Siswa)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Klik status untuk mengubah",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // 5. Student List
            if (filteredEntries.isEmpty()) {
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
                            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tidak ada siswa yang ditemukan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            Text("Pastikan rombel kelas sudah terisi data siswa dari Dapodik.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.studentId }) { entry ->
                    StudentAttendanceCard(
                        entry = entry,
                        onStatusChange = { newStatus ->
                            attendanceState[entry.studentId] = entry.copy(status = newStatus)
                        },
                        onNotesChange = { newNote ->
                            attendanceState[entry.studentId] = entry.copy(notes = newNote)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Dialog Riwayat Presensi
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text("Riwayat Presensi Tersimpan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenPrimary)
            },
            text = {
                val groupHistory = remember(attendanceRecords) {
                    attendanceRecords.groupBy { "${it.date} • ${it.className} (${it.subject})" }
                }
                if (groupHistory.isEmpty()) {
                    Text("Belum ada riwayat presensi yang tersimpan.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupHistory.forEach { (sessionKey, list) ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = sessionKey, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenSecondary)
                                        val h = list.count { it.status == AttendanceStatus.HADIR }
                                        val s = list.count { it.status == AttendanceStatus.SAKIT }
                                        val i = list.count { it.status == AttendanceStatus.IZIN }
                                        val a = list.count { it.status == AttendanceStatus.ALPA }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Hadir: $h | Sakit: $s | Izin: $i | Alpa: $a (${list.size} total)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Tutup", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AttendanceCountBadge(
    title: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun StudentAttendanceCard(
    entry: AttendanceStudentEntry,
    onStatusChange: (AttendanceStatus) -> Unit,
    onNotesChange: (String) -> Unit
) {
    var showNoteInput by remember { mutableStateOf(entry.notes.isNotBlank()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_student_${entry.studentId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            when (entry.status) {
                AttendanceStatus.HADIR -> Color(0xFF10B981).copy(alpha = 0.35f)
                AttendanceStatus.SAKIT -> Color(0xFFF59E0B).copy(alpha = 0.45f)
                AttendanceStatus.IZIN -> Color(0xFF0284C7).copy(alpha = 0.45f)
                AttendanceStatus.ALPA -> Color(0xFFEF4444).copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Gender Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (entry.gender == "P") Color(0xFFF472B6) else Color(0xFF38BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.gender,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = entry.studentName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.notes.isNotBlank()) {
                            Text(
                                text = "Ket: ${entry.notes}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Note toggle icon
                IconButton(
                    onClick = { showNoteInput = !showNoteInput },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (showNoteInput) Icons.Default.EditNote else Icons.Outlined.EditNote,
                        contentDescription = "Catatan",
                        tint = if (entry.notes.isNotBlank()) GoldAccent else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Buttons Row: Hadir, Sakit, Izin, Alpa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceStatus.values().forEach { status ->
                    val isSelected = entry.status == status
                    val containerColor = if (isSelected) Color(status.colorHex) else Color(status.colorHex).copy(alpha = 0.08f)
                    val contentColor = if (isSelected) Color.White else Color(status.colorHex)

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStatusChange(status) },
                        color = containerColor,
                        shape = RoundedCornerShape(8.dp),
                        border = if (!isSelected) BorderStroke(1.dp, Color(status.colorHex).copy(alpha = 0.3f)) else null
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = status.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            // Expandable note input
            if (showNoteInput) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = entry.notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("Alasan sakit/izin/keterangan...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    }
}
