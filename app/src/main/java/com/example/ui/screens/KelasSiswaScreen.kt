package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.ClassRoom
import com.example.data.models.Student
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelasSiswaScreen(
    viewModel: AppViewModel,
    classes: List<ClassRoom>,
    students: List<Student>
) {
    val context = LocalContext.current
    val searchQuery by viewModel.studentSearchQuery.collectAsStateWithLifecycle()
    val searchedStudents by viewModel.searchedStudents.collectAsStateWithLifecycle()

    var selectedClassId by remember { mutableStateOf<Int?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedClassId?.let { classId ->
                viewModel.importStudentsFromFile(
                    classId = classId,
                    uri = it,
                    onSuccess = { count ->
                        Toast.makeText(context, "Berhasil mengimpor $count data siswa", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Gagal mengimpor: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportStudentsToJson(
                classId = selectedClassId,
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Data siswa berhasil diekspor", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengekspor: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val downloadTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        uri?.let {
            viewModel.exportExcelTemplate(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Template Excel (.csv) berhasil diunduh!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengunduh template: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showImportStudentDialog by remember { mutableStateOf(false) }
    var showClearStudentConfirmDialog by remember { mutableStateOf(false) }
    var importRawText by remember { mutableStateOf("") }

    // Forms states
    var newClassName by remember { mutableStateOf("") }
    var newClassLevel by remember { mutableStateOf("I") }

    var studentNis by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var studentGender by remember { mutableStateOf("L") }
    var studentParentName by remember { mutableStateOf("") }
    var studentParentPhone by remember { mutableStateOf("") }

    // Default selection to first class if available
    LaunchedEffect(classes) {
        if (selectedClassId == null && classes.isNotEmpty()) {
            selectedClassId = classes.first().id
        }
    }

    val activeClass = classes.find { it.id == selectedClassId }
    val filteredStudents = if (selectedClassId != null) {
        searchedStudents.filter { it.classId == selectedClassId }
    } else {
        searchedStudents
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Classroom Selection Row
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Kelas Diampu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Button(
                        onClick = { showAddClassDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Kelas", fontSize = 11.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                if (classes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada kelas. Silakan tambah kelas baru.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Semua Kelas" chip
                        ElevatedFilterChip(
                            selected = selectedClassId == null,
                            onClick = { selectedClassId = null },
                            label = { Text("Semua (${students.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        classes.forEach { cls ->
                            val isSelected = cls.id == selectedClassId
                            val count = students.count { it.classId == cls.id }
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { selectedClassId = cls.id },
                                label = { Text("${cls.name} ($count)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Dapodik Info & Sync Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Data Siswa SD Negeri Kiarapandak 01",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Tersedia ${classes.size} Rombel • ${students.size} Siswa Aktif",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.loadFullDapodikStudents(force = true) { count ->
                                Toast.makeText(context, "Berhasil memuat $count data siswa Dapodik!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sinkronkan", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // 2. Class Operations (Details, Delete, etc.)
        if (activeClass != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = activeClass.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                            Text(text = "Tingkat/Fase: ${activeClass.level} • Jumlah Siswa: ${filteredStudents.size}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Row {
                            IconButton(onClick = {
                                viewModel.deleteClass(activeClass)
                                selectedClassId = null
                                Toast.makeText(context, "Kelas dihapus", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Kelas", tint = Color(0xFFE57373))
                            }
                        }
                    }
                }
            }
        }

        // 3. Search and Operations Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setStudentSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input"),
                    placeholder = { Text("Cari nama siswa atau NIS...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setStudentSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Actions: Add Student, Import/Export
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showAddStudentDialog = true
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Siswa Baru", fontSize = 13.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            if (selectedClassId == null) {
                                selectedClassId = classes.firstOrNull()?.id
                            }
                            showImportStudentDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        border = BorderStroke(1.dp, GreenPrimary)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Impor", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val className = activeClass?.name?.replace(" ", "_") ?: "Semua_Siswa_SDN_Kiarapandak_01"
                            exportLauncher.launch("Siswa_${className}.json")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        border = BorderStroke(1.dp, GreenPrimary)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor", fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Students list header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Peserta Didik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )

                if (students.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearStudentConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        border = BorderStroke(1.dp, Color(0xFFEF5350)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("clear_student_data_button")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Bersihkan Data", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bersihkan Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Students Card list
        if (filteredStudents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Siswa tidak ditemukan.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredStudents) { student ->
                val studentClassName = classes.find { it.id == student.classId }?.name ?: "Kelas"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (student.gender == "L") Color(0xFFBBDEFB) else Color(0xFFF8BBD0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (student.gender == "L") "L" else "P",
                                color = if (student.gender == "L") Color(0xFF1976D2) else Color(0xFFC2185B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = student.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GreenPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = studentClassName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GreenPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            val nisDisplay = buildString {
                                append("NIS: ${student.nis}")
                                if (student.nisn.isNotBlank() && student.nisn != "0") {
                                    append(" • NISN: ${student.nisn}")
                                }
                            }
                            Text(text = nisDisplay, fontSize = 11.sp, color = Color.Gray)

                            if (student.birthPlace.isNotBlank() && student.birthDate.isNotBlank() && student.birthDate != "-") {
                                Text(
                                    text = "TTL: ${student.birthPlace}, ${student.birthDate}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }

                            if (student.parentName.isNotBlank() && student.parentName != "-") {
                                Text(
                                    text = "Orang Tua / Wali: ${student.parentName}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }

                            if (student.address.isNotBlank() && student.address != "-") {
                                Text(
                                    text = "Alamat: ${student.address}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            if (student.parentPhone.isNotBlank() && student.parentPhone != "-") {
                                Text(
                                    text = "No. HP / WA: ${student.parentPhone}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GreenPrimary
                                )
                            }
                        }
                        IconButton(onClick = {
                            viewModel.deleteStudent(student)
                            Toast.makeText(context, "Siswa dihapus", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Siswa", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Add Class Dialog
    if (showAddClassDialog) {
        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = { Text("Tambah Kelas Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text("Nama Kelas") },
                        placeholder = { Text("misal: Kelas IV-B") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newClassLevel,
                        onValueChange = { newClassLevel = it },
                        label = { Text("Tingkatan (I - VI)") },
                        placeholder = { Text("misal: IV") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClassName.isNotBlank() && newClassLevel.isNotBlank()) {
                            viewModel.addClass(newClassName, newClassLevel)
                            showAddClassDialog = false
                            newClassName = ""
                            newClassLevel = "I"
                            Toast.makeText(context, "Kelas baru berhasil dibuat", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Tambah Siswa Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = studentNis,
                        onValueChange = { studentNis = it },
                        label = { Text("NIS / Nomor Induk") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Gender selection
                    Text("Jenis Kelamin:", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = studentGender == "L", onClick = { studentGender = "L" })
                            Text("Laki-laki (L)", fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = studentGender == "P", onClick = { studentGender = "P" })
                            Text("Perempuan (P)", fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = studentParentName,
                        onValueChange = { studentParentName = it },
                        label = { Text("Nama Orang Tua / Wali") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = studentParentPhone,
                        onValueChange = { studentParentPhone = it },
                        label = { Text("No. HP Orang Tua (WhatsApp)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentClassId = selectedClassId ?: classes.firstOrNull()?.id
                        if (currentClassId != null && studentName.isNotBlank() && studentNis.isNotBlank()) {
                            viewModel.addStudent(
                                classId = currentClassId,
                                nis = studentNis,
                                name = studentName,
                                gender = studentGender,
                                parentName = studentParentName,
                                parentPhone = studentParentPhone
                            )
                            showAddStudentDialog = false
                            studentNis = ""
                            studentName = ""
                            studentGender = "L"
                            studentParentName = ""
                            studentParentPhone = ""
                            Toast.makeText(context, "Siswa berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        } else if (studentName.isBlank() || studentNis.isBlank()) {
                            Toast.makeText(context, "Mohon lengkapi NIS dan Nama Siswa", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Import Student Dialog
    if (showImportStudentDialog) {
        AlertDialog(
            onDismissRequest = { showImportStudentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = GreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Form Impor Data Siswa", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Silakan pilih salah satu cara untuk mengimpor siswa ke kelas ${activeClass?.name ?: ""}:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Option A: Upload File / Excel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Metode A: Impor Format Excel (.csv) / File",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Gunakan file spreadsheet Excel yang disimpan sebagai format CSV. Kolom template harus berurutan: NIS;Nama;Gender (L/P);Nama Orang Tua;No HP.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Download Template Button
                            OutlinedButton(
                                onClick = {
                                    showImportStudentDialog = false
                                    downloadTemplateLauncher.launch("Template_Impor_Siswa.csv")
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                                border = BorderStroke(1.dp, GreenPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unduh Template Excel (.csv)", fontSize = 12.sp)
                            }

                            // Upload Button
                            Button(
                                onClick = {
                                    showImportStudentDialog = false
                                    importLauncher.launch(arrayOf(
                                        "application/json", 
                                        "text/plain", 
                                        "text/comma-separated-values", 
                                        "text/csv", 
                                        "application/csv", 
                                        "application/octet-stream", 
                                        "application/vnd.ms-excel", 
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    ))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih & Unggah File Excel/CSV...", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }

                    // Divider/Atau
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                        Text(
                            text = " ATAU ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    }

                    // Option B: Tempel Teks Manual
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Metode B: Tempel Teks Manual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Format: NIS;Nama;Gender(L/P);NamaOrtu;NoHP\n(Gunakan titik koma ';' atau koma ',' sebagai pemisah. Pisahkan baris per siswa)",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            lineHeight = 14.sp
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Contoh:\n10243;Ahmad Zaki;L;M. Yusuf;08123456789\n10244;Siti Aminah;P;Rahmat;08112233445",
                                fontSize = 9.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(8.dp),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        OutlinedTextField(
                            value = importRawText,
                            onValueChange = { importRawText = it },
                            placeholder = { Text("Tempel data siswa di sini...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentClassId = selectedClassId
                        if (currentClassId != null && importRawText.isNotBlank()) {
                            viewModel.importStudentsFromText(
                                classId = currentClassId,
                                text = importRawText,
                                onSuccess = { count ->
                                    showImportStudentDialog = false
                                    importRawText = ""
                                    Toast.makeText(context, "Berhasil mengimpor $count siswa!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Gagal mengimpor: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else if (importRawText.isBlank()) {
                            Toast.makeText(context, "Masukkan teks atau gunakan Metode A", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = importRawText.isNotBlank()
                ) {
                    Text("Proses Impor", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportStudentDialog = false
                        importRawText = ""
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Clear Student Data Confirmation Dialog
    if (showClearStudentConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearStudentConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Bersihkan Data Siswa",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pilih opsi pembersihan data siswa di bawah ini. Tindakan ini akan menghapus data siswa secara permanen dari database lokal.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    if (activeClass != null) {
                        Button(
                            onClick = {
                                viewModel.clearStudentsInClass(activeClass.id, activeClass.name)
                                showClearStudentConfirmDialog = false
                                Toast.makeText(context, "Data siswa di ${activeClass.name} berhasil dibersihkan", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hapus Siswa di ${activeClass.name} (${filteredStudents.size} Siswa)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllStudents()
                            showClearStudentConfirmDialog = false
                            Toast.makeText(context, "Seluruh data siswa dari semua kelas berhasil dibersihkan", Toast.LENGTH_LONG).show()
                        },
                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hapus Seluruh Siswa (${students.size} Siswa)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClearStudentConfirmDialog = false }) {
                    Text("Batal", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
