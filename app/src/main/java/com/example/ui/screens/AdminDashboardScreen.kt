package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.data.repository.SyncBannerState
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentAppRole.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val managedUsers by viewModel.managedUsers.collectAsStateWithLifecycle()
    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val journals by viewModel.allJournals.collectAsStateWithLifecycle()
    val assessments by viewModel.allAssessments.collectAsStateWithLifecycle()
    val questions by viewModel.allQuestions.collectAsStateWithLifecycle()
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val pendingOps by viewModel.pendingOperations.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: CRUD Data, 1: Pengguna & Hak Akses, 2: Riwayat Aktivitas, 3: Antrean Sync
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntityFilter by remember { mutableStateOf("Siswa") }

    // Dialog states for Admin CRUD
    var showAddOrEditStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showAddOrEditClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<ClassRoom?>(null) }
    var editingUser by remember { mutableStateOf<ManagedUserProfile?>(null) }

    val entityCategories = listOf("Siswa", "Kelas", "Dokumen", "Jurnal", "Penilaian", "Bank Soal", "Kegiatan")

    LaunchedEffect(Unit) {
        viewModel.refreshUsers()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Summary Card
            Surface(
                color = GreenPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dashboard Admin & Manajemen Multi-Perangkat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GreenSecondary
                        )
                        Text(
                            text = "Login: ${currentProfile?.email ?: "-"} • Hak Akses: ${currentRole.displayName}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            viewModel.retryPendingSync { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("admin_sync_now_button")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (pendingOps.isNotEmpty()) "Sync (${pendingOps.size})" else "Sync", fontSize = 12.sp)
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                contentColor = GreenPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Kelola Data") },
                    icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_data")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pengguna (${managedUsers.size})") },
                    icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Riwayat Aktivitas") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_logs")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Antrean Sync (${pendingOps.size})") },
                    icon = { Icon(Icons.Default.SyncProblem, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_sync")
                )
            }

            when (selectedTab) {
                0 -> {
                    // Search & Entity Picker Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari di $selectedEntityFilter...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_search_input")
                            )

                            if (currentRole.canEditAcademicData && (selectedEntityFilter == "Siswa" || selectedEntityFilter == "Kelas")) {
                                Button(
                                    onClick = {
                                        if (selectedEntityFilter == "Siswa") {
                                            editingStudent = null
                                            showAddOrEditStudentDialog = true
                                        } else {
                                            editingClass = null
                                            showAddOrEditClassDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    modifier = Modifier.testTag("admin_add_record_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    if (isWideScreen) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tambah $selectedEntityFilter", color = Color.White)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            entityCategories.take(if (isWideScreen) 7 else 4).forEach { cat ->
                                FilterChip(
                                    selected = selectedEntityFilter == cat,
                                    onClick = { selectedEntityFilter = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }
                        if (!isWideScreen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                entityCategories.drop(4).forEach { cat ->
                                    FilterChip(
                                        selected = selectedEntityFilter == cat,
                                        onClick = { selectedEntityFilter = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        when (selectedEntityFilter) {
                            "Siswa" -> {
                                val filtered = students.filter {
                                    searchQuery.isBlank() ||
                                        it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.nis.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { student ->
                                    val clsName = classes.find { it.id == student.classId }?.name ?: "Kelas ${student.classId}"
                                    AdminRecordCard(
                                        title = student.name,
                                        subtitle = "NIS: ${student.nis.ifBlank { "-" }} • $clsName • ${if (student.gender == "P") "Perempuan" else "Laki-laki"}",
                                        detail = "Wali: ${student.parentName.ifBlank { "-" }} (${student.parentPhone.ifBlank { "-" }})",
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = {
                                            editingStudent = student
                                            showAddOrEditStudentDialog = true
                                        },
                                        onDelete = { viewModel.deleteStudent(student) }
                                    )
                                }
                            }
                            "Kelas" -> {
                                val filtered = classes.filter {
                                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { cls ->
                                    val count = students.count { it.classId == cls.id }
                                    AdminRecordCard(
                                        title = cls.name,
                                        subtitle = "Tingkat/Fase: ${cls.level}",
                                        detail = "$count Siswa terdaftar",
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = {
                                            editingClass = cls
                                            showAddOrEditClassDialog = true
                                        },
                                        onDelete = { viewModel.deleteClass(cls) }
                                    )
                                }
                            }
                            "Dokumen" -> {
                                val filtered = documents.filter {
                                    searchQuery.isBlank() ||
                                        it.title.contains(searchQuery, ignoreCase = true) ||
                                        it.type.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { doc ->
                                    AdminRecordCard(
                                        title = doc.title,
                                        subtitle = "${doc.type} • Kelas ${doc.classLevel} • ${doc.semester}",
                                        detail = "Penyusun: ${doc.authorTeacher}",
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = null,
                                        onDelete = { viewModel.deleteDocument(doc) }
                                    )
                                }
                            }
                            "Jurnal" -> {
                                val filtered = journals.filter {
                                    searchQuery.isBlank() ||
                                        it.topic.contains(searchQuery, ignoreCase = true) ||
                                        it.className.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { j ->
                                    AdminRecordCard(
                                        title = j.topic,
                                        subtitle = "${j.date} • ${j.className}",
                                        detail = j.activity,
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = null,
                                        onDelete = { viewModel.deleteJournal(j) }
                                    )
                                }
                            }
                            "Penilaian" -> {
                                val filtered = assessments.filter {
                                    searchQuery.isBlank() ||
                                        it.studentName.contains(searchQuery, ignoreCase = true) ||
                                        it.topic.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { a ->
                                    AdminRecordCard(
                                        title = "${a.studentName} — Nilai ${a.score}",
                                        subtitle = "${a.type} • ${a.topic}",
                                        detail = a.description,
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = null,
                                        onDelete = { viewModel.deleteAssessment(a) }
                                    )
                                }
                            }
                            "Bank Soal" -> {
                                val filtered = questions.filter {
                                    searchQuery.isBlank() || it.text.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { q ->
                                    AdminRecordCard(
                                        title = q.text,
                                        subtitle = "${q.type} • ${q.difficulty} • Kunci: ${q.answerKey}",
                                        detail = q.explanation,
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = null,
                                        onDelete = { viewModel.deleteQuestion(q) }
                                    )
                                }
                            }
                            else -> {
                                val filtered = activities.filter {
                                    searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
                                }
                                items(filtered, key = { it.syncKey }) { act ->
                                    AdminRecordCard(
                                        title = act.title,
                                        subtitle = "${act.date} • ${act.type} • ${act.participantCount} peserta",
                                        detail = act.description,
                                        canModify = currentRole.canEditAcademicData,
                                        isWide = isWideScreen,
                                        onEdit = null,
                                        onDelete = { viewModel.deleteReligiousActivity(act) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // User & Role Management
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pengelolaan Pengguna & Hak Akses", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GreenSecondary)
                                Text(
                                    text = if (currentRole.isAdminOrAbove) {
                                        "Atur peran Super Admin, Admin, Guru, atau Siswa secara aman di PostgreSQL."
                                    } else {
                                        "Hanya Super Admin dan Admin yang dapat mengubah peran pengguna."
                                    },
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { viewModel.refreshUsers() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang pengguna", tint = GreenPrimary)
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(managedUsers, key = { it.id }) { user ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (user.isActive) GreenPrimary.copy(alpha = 0.14f) else Color(0xFFFEE2E2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (user.isActive) GreenPrimary else Color(0xFFDC2626)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(user.email, fontSize = 11.sp, color = Color.Gray)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text(user.appRole.displayName, fontSize = 10.sp) }
                                                )
                                                if (!user.isActive) {
                                                    AssistChip(
                                                        onClick = {},
                                                        label = { Text("Nonaktif", fontSize = 10.sp, color = Color(0xFFDC2626)) }
                                                    )
                                                }
                                            }
                                        }
                                        if (currentRole.isAdminOrAbove) {
                                            IconButton(
                                                onClick = { editingUser = user },
                                                modifier = Modifier.testTag("edit_user_role_${user.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Ubah hak akses", tint = GreenPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Activity History
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(recentLogs) { log ->
                            val timeStr = remember(log.timestamp) {
                                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${log.userName.ifBlank { log.userType }} (${log.userType})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = GreenPrimary
                                        )
                                        Text(timeStr, fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(log.text, fontSize = 13.sp)
                                    if (log.deviceInfo.isNotBlank() || log.userEmail.isNotBlank()) {
                                        Text(
                                            text = "${log.userEmail.ifBlank { "-" }} • ${log.deviceInfo}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Offline Queue & Retry Monitor
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isOnline) "Jaringan Terhubung ke Supabase" else "Koneksi Offline / Tidak Stabil",
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSecondary
                                )
                                Text(
                                    text = when (val st = syncState) {
                                        is SyncBannerState.Idle -> "Siap sinkronisasi."
                                        is SyncBannerState.Syncing -> "Sedang menyinkronkan data dengan Supabase..."
                                        is SyncBannerState.Synced -> "Tersinkronisasi otomatis pada ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(st.lastSyncedAt))}"
                                        is SyncBannerState.OfflineQueued -> st.message
                                        is SyncBannerState.Error -> "Kendala: ${st.message}"
                                    },
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = {
                                        viewModel.retryPendingSync { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Coba Ulang Antrean Sekarang", color = Color.White)
                                }
                            }
                        }

                        if (pendingOps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Semua perubahan sudah tersinkronisasi ke database cloud.", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(pendingOps, key = { it.id }) { op ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "${op.operation} • ${op.tableName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text("Key: ${op.recordKey} • Percobaan ulang: ${op.retryCount}", fontSize = 11.sp, color = Color.Gray)
                                            if (!op.lastError.isNullOrBlank()) {
                                                Text("Error terakhir: ${op.lastError}", fontSize = 11.sp, color = Color(0xFFDC2626))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Student Dialog
    if (showAddOrEditStudentDialog) {
        var name by remember(editingStudent) { mutableStateOf(editingStudent?.name ?: "") }
        var nis by remember(editingStudent) { mutableStateOf(editingStudent?.nis ?: "") }
        var gender by remember(editingStudent) { mutableStateOf(editingStudent?.gender ?: "L") }
        var parentName by remember(editingStudent) { mutableStateOf(editingStudent?.parentName ?: "") }
        var parentPhone by remember(editingStudent) { mutableStateOf(editingStudent?.parentPhone ?: "") }
        var selectedClassId by remember(editingStudent) {
            mutableIntStateOf(editingStudent?.classId ?: classes.firstOrNull()?.id ?: 1)
        }

        AlertDialog(
            onDismissRequest = { showAddOrEditStudentDialog = false },
            title = { Text(if (editingStudent == null) "Tambah Siswa Baru" else "Edit Data Siswa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Siswa") }, singleLine = true)
                    OutlinedTextField(value = nis, onValueChange = { nis = it }, label = { Text("NIS") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = gender == "L", onClick = { gender = "L" }, label = { Text("Laki-laki") })
                        FilterChip(selected = gender == "P", onClick = { gender = "P" }, label = { Text("Perempuan") })
                    }
                    OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Nama Orang Tua") }, singleLine = true)
                    OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = { Text("No HP Orang Tua") }, singleLine = true)
                    if (classes.isNotEmpty()) {
                        Text("Kelas: ${classes.find { it.id == selectedClassId }?.name ?: "-"}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Nama siswa wajib diisi", Toast.LENGTH_SHORT).show()
                        } else {
                            val current = editingStudent
                            if (current == null) {
                                viewModel.addStudent(selectedClassId, nis, name, gender, parentName, parentPhone)
                            } else {
                                viewModel.updateStudent(
                                    current.copy(
                                        name = name.trim(),
                                        nis = nis.trim(),
                                        gender = gender,
                                        parentName = parentName.trim(),
                                        parentPhone = parentPhone.trim()
                                    )
                                )
                            }
                            showAddOrEditStudentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOrEditStudentDialog = false }) { Text("Batal") }
            }
        )
    }

    // Add / Edit Class Dialog
    if (showAddOrEditClassDialog) {
        var clsName by remember(editingClass) { mutableStateOf(editingClass?.name ?: "") }
        var clsLevel by remember(editingClass) { mutableStateOf(editingClass?.level ?: "I") }

        AlertDialog(
            onDismissRequest = { showAddOrEditClassDialog = false },
            title = { Text(if (editingClass == null) "Tambah Kelas Baru" else "Edit Kelas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = clsName, onValueChange = { clsName = it }, label = { Text("Nama Kelas (mis. Kelas 4-A)") }, singleLine = true)
                    OutlinedTextField(value = clsLevel, onValueChange = { clsLevel = it }, label = { Text("Tingkat (I - VI)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clsName.isBlank()) {
                            Toast.makeText(context, "Nama kelas wajib diisi", Toast.LENGTH_SHORT).show()
                        } else {
                            val current = editingClass
                            if (current == null) {
                                viewModel.addClass(clsName, clsLevel)
                            } else {
                                viewModel.updateClass(current.copy(name = clsName.trim(), level = clsLevel.trim()))
                            }
                            showAddOrEditClassDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOrEditClassDialog = false }) { Text("Batal") }
            }
        )
    }

    // Edit User Role & Active Status Dialog
    editingUser?.let { target ->
        var selectedRole by remember(target) { mutableStateOf(target.appRole) }
        var isActive by remember(target) { mutableStateOf(target.isActive) }
        val availableRoles = if (currentRole == AppRole.SUPER_ADMIN) {
            AppRole.entries
        } else {
            listOf(AppRole.ADMIN, AppRole.GURU, AppRole.SISWA)
        }

        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("Atur Hak Akses Pengguna") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${target.fullName}\n${target.email}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    availableRoles.forEach { roleOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedRole == roleOption,
                                onClick = { selectedRole = roleOption }
                            )
                            Text(roleOption.displayName, fontSize = 13.sp)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Status Akun Aktif", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserRoleAndStatus(target, selectedRole, isActive) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (ok) editingUser = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan Hak Akses", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun AdminRecordCard(
    title: String,
    subtitle: String,
    detail: String,
    canModify: Boolean,
    isWide: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 11.sp, color = GreenPrimary)
                if (detail.isNotBlank()) {
                    Text(detail, fontSize = 11.sp, color = Color.Gray, maxLines = if (isWide) 2 else 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (canModify) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit data", tint = GreenPrimary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus data", tint = Color(0xFFDC2626))
                }
            }
        }
    }
}
