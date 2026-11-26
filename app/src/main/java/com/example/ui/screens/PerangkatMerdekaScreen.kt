package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import com.example.data.models.MerdekaDocument
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerangkatMerdekaScreen(
    viewModel: AppViewModel,
    documents: List<MerdekaDocument>
) {
    val context = LocalContext.current
    val isGenerating by viewModel.isPerangkatAiGenerating.collectAsStateWithLifecycle()
    val aiResponse by viewModel.perangkatAiResponse.collectAsStateWithLifecycle()
    val teacherProfile by viewModel.teacherProfile.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    // Dialog & Form States
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingDocument by remember { mutableStateOf<MerdekaDocument?>(null) }
    var viewingDocument by remember { mutableStateOf<MerdekaDocument?>(null) }
    var documentToDelete by remember { mutableStateOf<MerdekaDocument?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Form inputs
    var formType by remember { mutableStateOf("Modul Ajar") }
    var formTitle by remember { mutableStateOf("") }
    var formSubject by remember { mutableStateOf("Pendidikan Agama Islam dan Budi Pekerti") }
    var formClassLevel by remember { mutableStateOf("IV") }
    var formSemester by remember { mutableStateOf("Ganjil") }
    var formAcademicYear by remember { mutableStateOf("2026/2027") }
    var formAuthor by remember { mutableStateOf(teacherProfile.fullName) }
    var formContentText by remember { mutableStateOf("") }
    var formFileUri by remember { mutableStateOf<String?>(null) }
    var formFileName by remember { mutableStateOf<String?>(null) }
    var formFileSize by remember { mutableStateOf<String?>(null) }
    var formFileMimeType by remember { mutableStateOf<String?>(null) }

    // AI Generator Prompt Form
    var aiDocType by remember { mutableStateOf("Modul Ajar") }
    var aiTopic by remember { mutableStateOf("") }
    var aiClassLevel by remember { mutableStateOf("IV") }

    // File picker launcher for document upload
    val uploadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            formFileUri = uri.toString()
            val meta = getFileMetadata(context, uri)
            formFileName = meta.first
            formFileSize = meta.second
            formFileMimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            
            if (formTitle.isBlank()) {
                formTitle = meta.first.substringBeforeLast(".")
            }
            if (formType == "Modul Ajar" && meta.first.lowercase().contains("atp")) {
                formType = "ATP"
            } else if (formType == "Modul Ajar" && meta.first.lowercase().contains("kktp")) {
                formType = "KKTP"
            }
            Toast.makeText(context, "Berkas '${meta.first}' berhasil dipilih (${meta.second})", Toast.LENGTH_SHORT).show()
        }
    }

    // Export HTML launcher
    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        uri?.let {
            viewingDocument?.let { doc ->
                viewModel.exportDocument(
                    doc = doc,
                    uri = it,
                    format = "html",
                    onSuccess = {
                        Toast.makeText(context, "Dokumen HTML berhasil diekspor! Buka file untuk mencetak ke PDF", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Gagal mengekspor: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // Export TXT launcher
    val exportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            viewingDocument?.let { doc ->
                viewModel.exportDocument(
                    doc = doc,
                    uri = it,
                    format = "txt",
                    onSuccess = {
                        Toast.makeText(context, "Dokumen teks berhasil diekspor ke perangkat", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Gagal mengekspor: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val categories = listOf("Semua", "Modul Ajar", "ATP", "CP", "Prota", "Promes", "KKTP", "Bahan Ajar", "LKPD", "Dokumen Unggahan")
    val formCategories = listOf("Modul Ajar", "ATP", "CP", "Prota", "Promes", "KKTP", "Bahan Ajar", "LKPD", "Dokumen Unggahan", "Asesmen", "Refleksi")

    // Filter and search logic
    val filteredDocuments = documents.filter { doc ->
        val matchesCategory = if (selectedCategory == "Semua") {
            true
        } else if (selectedCategory == "Dokumen Unggahan") {
            doc.fileUri != null || doc.type == "Dokumen Unggahan"
        } else {
            doc.type == selectedCategory
        }
        val matchesQuery = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.contentText.contains(searchQuery, ignoreCase = true) ||
                doc.type.contains(searchQuery, ignoreCase = true) ||
                (doc.fileName?.contains(searchQuery, ignoreCase = true) == true) ||
                doc.classLevel.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    // Helper to open form for create
    fun openCreateForm(presetType: String = "Modul Ajar") {
        editingDocument = null
        formType = presetType
        formTitle = ""
        formSubject = "Pendidikan Agama Islam dan Budi Pekerti"
        formClassLevel = "IV"
        formSemester = "Ganjil"
        formAcademicYear = "2026/2027"
        formAuthor = teacherProfile.fullName
        formContentText = ""
        formFileUri = null
        formFileName = null
        formFileSize = null
        formFileMimeType = null
        showAddOrEditDialog = true
    }

    // Helper to open form for edit
    fun openEditForm(doc: MerdekaDocument) {
        editingDocument = doc
        formType = doc.type
        formTitle = doc.title
        formSubject = doc.subject
        formClassLevel = doc.classLevel
        formSemester = doc.semester
        formAcademicYear = doc.academicYear
        formAuthor = doc.authorTeacher
        formContentText = doc.contentText
        formFileUri = doc.fileUri
        formFileName = doc.fileName
        formFileSize = doc.fileSize
        formFileMimeType = doc.fileMimeType
        showAddOrEditDialog = true
    }

    // Capture generated AI response for Perangkat Merdeka
    LaunchedEffect(aiResponse) {
        if (aiResponse.isNotBlank()) {
            val cleanTitleTopic = aiTopic.trim().ifEmpty { "Kurikulum Merdeka PAI" }
            openCreateForm(presetType = aiDocType)
            formTitle = "$aiDocType - $cleanTitleTopic (Kelas $aiClassLevel)"
            formClassLevel = aiClassLevel
            formContentText = aiResponse
            viewModel.clearPerangkatAiResponse()
            Toast.makeText(context, "Draf $aiDocType berhasil disusun! Silakan tinjau & simpan.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header with Stats & Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = "Perangkat & Dokumen Ajar PAI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kelola ${documents.size} berkas modul ajar & kurikulum",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Button(
                            onClick = { openCreateForm() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buat / Unggah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Berkas",
                            value = "${documents.size}",
                            modifier = Modifier.weight(1f),
                            color = GreenPrimary
                        )
                        StatCard(
                            label = "Modul Ajar",
                            value = "${documents.count { it.type == "Modul Ajar" }}",
                            modifier = Modifier.weight(1f),
                            color = GoldAccent
                        )
                        StatCard(
                            label = "Dokumen File",
                            value = "${documents.count { it.fileUri != null }}",
                            modifier = Modifier.weight(1f),
                            color = GreenSecondary
                        )
                    }
                }
            }
        }

        // 2. AI Generator Quick Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generator Administrasi Otomatis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GreenPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GreenPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "BSKAP 020/2026",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Standar regulasi: Keputusan Kepala BSKAP No. 020 Tahun 2026",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Modul Ajar", "ATP", "KKTP", "Asesmen", "Bahan Ajar").forEach { type ->
                            FilterChip(
                                selected = aiDocType == type,
                                onClick = { aiDocType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = aiTopic,
                            onValueChange = { aiTopic = it },
                            placeholder = { Text("Materi, misal: Zakat Fitrah & Mal") },
                            label = { Text("Tema / Materi Pokok") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = aiClassLevel,
                            onValueChange = { aiClassLevel = it },
                            label = { Text("Kelas") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (aiTopic.isBlank()) {
                                Toast.makeText(context, "Silakan isi topik pembelajaran terlebih dahulu", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.generatePerangkatWithAI(aiDocType, aiTopic, aiClassLevel)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("ai_generate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang menyusun draft perangkat...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Susun Dokumen Kurikulum Merdeka", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Search & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari judul dokumen, materi, atau berkas...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        val count = if (category == "Semua") {
                            documents.size
                        } else if (category == "Dokumen Unggahan") {
                            documents.count { it.fileUri != null || it.type == "Dokumen Unggahan" }
                        } else {
                            documents.count { it.type == category }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text("$category ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 4. Documents List
        if (filteredDocuments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada dokumen yang cocok dengan kata kunci '$searchQuery'." else "Belum ada dokumen pada kategori '$selectedCategory'.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { openCreateForm(presetType = if (selectedCategory == "Semua") "Modul Ajar" else selectedCategory) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buat Dokumen Baru")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredDocuments, key = { it.id }) { doc ->
                DocumentCard(
                    doc = doc,
                    onView = { viewingDocument = doc },
                    onEdit = { openEditForm(doc) },
                    onDelete = { documentToDelete = doc },
                    onOpenFile = {
                        if (doc.fileUri != null) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(doc.fileUri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak dapat membuka berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // --- MODAL: CREATE / EDIT / UPLOAD DOCUMENT DIALOG ---
    if (showAddOrEditDialog) {
        val isEditing = editingDocument != null
        AlertDialog(
            onDismissRequest = { showAddOrEditDialog = false },
            title = {
                Text(
                    text = if (isEditing) "Sunting Dokumen Pembelajaran" else "Buat / Unggah Dokumen Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = GreenPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category selector chips
                    Text("Kategori / Jenis Perangkat:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        formCategories.forEach { type ->
                            FilterChip(
                                selected = formType == type,
                                onClick = { formType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Template Presets Quick Fill
                    if (!isEditing) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Format Cepat (CP 020/2026):", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                                SuggestionChip(
                                    onClick = {
                                        formType = "CP"
                                        formTitle = "Capaian Pembelajaran (CP) PAI & BP SD"
                                        formClassLevel = "Semua"
                                        formContentText = generateSampleCp()
                                    },
                                    label = { Text("CP 020/2026 Resmi", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        formType = "Modul Ajar"
                                        formTitle = "Modul Ajar PAI Fase B Kelas IV (CP 020/2026)"
                                        formClassLevel = "IV"
                                        formContentText = generateSampleModulAjar()
                                    },
                                    label = { Text("Modul Ajar (CP 020)", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        formType = "ATP"
                                        formTitle = "Alur Tujuan Pembelajaran (ATP) PAI (CP 020/2026)"
                                        formClassLevel = "IV"
                                        formContentText = generateSampleAtp()
                                    },
                                    label = { Text("ATP (CP 020)", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        formType = "KKTP"
                                        formTitle = "KKTP Taksonomi SOLO PAI (CP 020/2026)"
                                        formClassLevel = "IV"
                                        formContentText = generateSampleKktp()
                                    },
                                    label = { Text("KKTP SOLO (CP 020)", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Judul Dokumen / Perangkat Ajar *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formClassLevel,
                            onValueChange = { formClassLevel = it },
                            label = { Text("Tingkat Kelas (I-VI)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formSemester,
                            onValueChange = { formSemester = it },
                            label = { Text("Semester") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formAcademicYear,
                            onValueChange = { formAcademicYear = it },
                            label = { Text("Tahun Ajaran") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formAuthor,
                            onValueChange = { formAuthor = it },
                            label = { Text("Guru Pengampu") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Attached File Section (Upload / Clear)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lampiran Berkas / Dokumen", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                OutlinedButton(
                                    onClick = { uploadFileLauncher.launch("*/*") },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (formFileUri == null) "Pilih Berkas" else "Ganti Berkas", fontSize = 11.sp)
                                }
                            }

                            if (formFileUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formFileName ?: "Berkas Lampiran",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formFileSize ?: "Ukuran tidak diketahui"} • ${formFileMimeType ?: "File"}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            formFileUri = null
                                            formFileName = null
                                            formFileSize = null
                                            formFileMimeType = null
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus lampiran", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formContentText,
                        onValueChange = { formContentText = it },
                        label = { Text("Isi / Catatan Perangkat Ajar") },
                        placeholder = { Text("Tuliskan deskripsi kompetensi, alur tujuan, langkah pembelajaran, atau ringkasan materi di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formTitle.isBlank()) {
                            Toast.makeText(context, "Judul dokumen wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isEditing) {
                            val updated = editingDocument!!.copy(
                                type = formType,
                                title = formTitle,
                                subject = formSubject,
                                classLevel = formClassLevel,
                                semester = formSemester,
                                academicYear = formAcademicYear,
                                authorTeacher = formAuthor,
                                contentText = formContentText,
                                fileUri = formFileUri,
                                fileName = formFileName,
                                fileSize = formFileSize,
                                fileMimeType = formFileMimeType
                            )
                            viewModel.updateDocument(updated)
                            Toast.makeText(context, "Dokumen '${formTitle}' berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addDocument(
                                type = formType,
                                title = formTitle,
                                subject = formSubject,
                                classLevel = formClassLevel,
                                semester = formSemester,
                                academicYear = formAcademicYear,
                                authorTeacher = formAuthor,
                                contentText = formContentText,
                                fileUri = formFileUri,
                                fileName = formFileName,
                                fileSize = formFileSize,
                                fileMimeType = formFileMimeType
                            )
                            Toast.makeText(context, "Dokumen '${formTitle}' berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        }
                        showAddOrEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(if (isEditing) "Simpan Perubahan" else "Simpan Dokumen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOrEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- MODAL: VIEW DOCUMENT DETAILS ---
    if (viewingDocument != null) {
        val doc = viewingDocument!!
        val formattedDate = remember(doc.createdAt) {
            java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(doc.createdAt))
        }

        AlertDialog(
            onDismissRequest = { viewingDocument = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GreenPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = doc.type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = GreenPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metadata card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Mata Pelajaran: ${doc.subject}", fontSize = 12.sp, color = TextDark)
                            Text("Kelas: ${doc.classLevel} • Semester: ${doc.semester} • TA: ${doc.academicYear}", fontSize = 12.sp, color = TextDark)
                            Text("Pengampu: ${doc.authorTeacher}", fontSize = 12.sp, color = Color.Gray)
                            Text("Tanggal Dibuat: $formattedDate", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // Attached file notice
                    if (doc.fileUri != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GreenSecondary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = GreenPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.fileName ?: "Berkas Lampiran", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${doc.fileSize ?: "File"} • ${doc.fileMimeType ?: ""}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse(doc.fileUri)
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Tidak dapat membuka lampiran: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                ) {
                                    Text("Buka", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Text("Konten / Rincian Dokumen:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenPrimary)
                    Text(
                        text = doc.contentText.ifEmpty { "(Tidak ada teks konten)" },
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextDark
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val fileName = "${doc.type}_${doc.title.replace(" ", "_")}.html"
                            exportHtmlLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak HTML", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val fileName = "${doc.type}_${doc.title.replace(" ", "_")}.txt"
                            exportTxtLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Teks", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            openEditForm(doc)
                            viewingDocument = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sunting", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingDocument = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // --- MODAL: CONFIRM SINGLE DELETE ---
    if (documentToDelete != null) {
        val targetDoc = documentToDelete!!
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Hapus Dokumen?") },
            text = { Text("Apakah Anda yakin ingin menghapus dokumen '${targetDoc.title}' (${targetDoc.type}) dari arsip administrasi?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(targetDoc)
                        documentToDelete = null
                        Toast.makeText(context, "Dokumen '${targetDoc.title}' telah dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun DocumentCard(
    doc: MerdekaDocument,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenPrimary.copy(alpha = 0.09f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = doc.type.take(4),
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kelas ${doc.classLevel} • Sem. ${doc.semester} • TA ${doc.academicYear}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Action Menu
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Sunting", tint = GreenPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            // Attached file chip if uploaded
            if (doc.fileUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenSecondary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .clickable { onOpenFile() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${doc.fileName ?: "Berkas Lampiran"} (${doc.fileSize ?: "File"})",
                        fontSize = 11.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Buka", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// Utility to inspect file metadata from content uri
private fun getFileMetadata(context: Context, uri: Uri): Pair<String, String> {
    var name = "dokumen_pembelajaran"
    var sizeStr = "0 KB"
    val cursor: android.database.Cursor? = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex != -1) {
                name = it.getString(nameIndex) ?: name
            }
            if (sizeIndex != -1) {
                val sizeBytes = it.getLong(sizeIndex)
                sizeStr = when {
                    sizeBytes > 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
                    sizeBytes > 1024 -> String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1024.0)
                    else -> "$sizeBytes B"
                }
            }
        }
    }
    return Pair(name, sizeStr)
}

// Quick Sample Templates - Berdasarkan Keputusan Kepala BSKAP No. 020 Tahun 2026
private fun generateSampleCp(): String {
    return """
        CAPAIAN PEMBELAJARAN (CP) PAI & BUDI PEKERTI SEKOLAH DASAR
        Sesuai Keputusan Kepala BSKAP Nomor 020 Tahun 2026
        (Perubahan atas Keputusan Kepala BSKAP No. 046/H/KR/2025)
        =======================================================
        
        I. PONDASI & LIMA DIMENSI CINTA:
        Pendidikan Agama Islam dan Budi Pekerti berakar pada penanaman akidah yang kokoh dan akhlak mulia melalui pembiasaan 'Lima Dimensi Cinta':
        1. Cinta Allah SWT dan Rasul-Nya (Mahabbatullah wa Rasulih)
        2. Cinta Al-Qur'an dan Hadis sebagai pedoman hidup
        3. Cinta Diri Sendiri dan Sesama Manusia (Ukhuwah Insaniyah)
        4. Cinta Tanah Air dan Bangsa (Hubbul Wathan)
        5. Cinta Lingkungan Hidup dan Alam Ciptaan Allah
        
        II. ELEMEN & CAPAIAN FASE B (KELAS III & IV SD):
        1. Al-Qur'an dan Hadis:
           Peserta didik mampu membaca, menulis, dan menghafal surah-surah pendek pilihan (termasuk Q.S. Al-Hujurat:13) dengan tartil serta memahami pesan pokok tentang keragaman manusia.
        2. Akidah:
           Peserta didik memahami sifat-sifat Allah SWT, rukun iman, dan mengamalkan nilai-nilai asmaulhusna dalam keseharian.
        3. Akhlak:
           Peserta didik menghormati orang tua dan guru, bersikap santun, membiasakan sikap toleran (tasamuh) serta menjaga kerukunan antarsesama.
        4. Fikih:
           Peserta didik memahami tanda-tanda usia baligh menurut fikih dan biologi, tata cara mandi wajib (janabah), serta tertib salat berjamaah.
        5. Sejarah Peradaban Islam (SPI):
           Peserta didik menceritakan keteladanan Nabi Muhammad SAW saat hijrah ke Madinah dan mempersaudarakan kaum Muhajirin dan Ansar.
        
        III. INTEGRASI TAKSONOMI SOLO:
        Perumusan kompetensi berfokus pada transisi pemahaman dari Multistruktural ke Relasional dan Abstrak Diperluas (Extended Abstract).
    """.trimIndent()
}

private fun generateSampleModulAjar(): String {
    return """
        MODUL AJAR PAI & BUDI PEKERTI (KURIKULUM MERDEKA)
        Pedoman Standar: Keputusan Kepala BSKAP Nomor 020 Tahun 2026
        =======================================================
        A. IDENTITAS UMUM
           - Penyusun        : Muhamad Suhendi, S.Pd
           - Instansi        : SD Negeri Kiarapandak 01
           - Jenjang / Fase  : SD / Fase B (Kelas IV)
           - Elemen          : Fikih & Akhlak Mulia (5 Dimensi Cinta)
           - Topik Pokok     : Menyambut Usia Baligh & Kewajiban Ibadah
           - Alokasi Waktu   : 3 JP (3 x 35 Menit)
           - Profil Pelajar  : Beriman, Mandiri, dan Bernalar Kritis

        B. CAPAIAN PEMBELAJARAN (CP 020/2026)
           Peserta didik memahami tanda-tanda baligh dalam pandangan fikih dan biologi, mempraktikkan tata cara bersuci dari hadas besar (taharah), serta memiliki kesadaran menjalankan kewajiban syariat dengan istiqamah.

        C. TUJUAN PEMBELAJARAN (TAKSONOMI SOLO - RELASIONAL)
           1. Mengidentifikasi tanda-tanda usia baligh bagi laki-laki dan perempuan secara tepat.
           2. Mempraktikkan rukun dan tata cara mandi wajib dengan tertib dan benar.
           3. Menghubungkan tanggung jawab aqil baligh dengan disiplin salat fardhu lima waktu (Dimensi Cinta Allah & Rasul-Nya).

        D. KEGIATAN PEMBELAJARAN
           1. Pendahuluan (15 Menit):
              - Salam, pembacaan doa, tadarus Q.S. Al-Hujurat:13.
              - Apersepsi: Guru mengaitkan tahapan pertumbuhan fisik dengan kedewasaan beribadah.
           2. Kegiatan Inti (75 Menit - Berdiferensiasi):
              - Eksplorasi: Mengamati infografis perbedaan tanda fisik dan tanda biologis baligh.
              - Kolaborasi: Diskusi kelompok menyusun peta konsep kewajiban setelah usia baligh.
              - Praktik Terbimbing: Demonstrasi tata cara mandi wajib sesuai sunah.
              - Refleksi Akhlak: Menulis jurnal pribadi tentang tekad menjaga salat dan aurat.
           3. Penutup (15 Menit):
              - Penguatan konsep, asesmen formatif mandiri, dan doa penutup majelis.

        E. ASESMEN PEMBELAJARAN
           - Asesmen Formatif: Observasi unjuk kerja dan lembar ceklis praktik mandi wajib.
           - Asesmen Sumatif : Penugasan tes tulis analisis kasus usia baligh (HOTS Level Relasional).
    """.trimIndent()
}

private fun generateSampleAtp(): String {
    return """
        ALUR TUJUAN PEMBELAJARAN (ATP) PAI & BUDI PEKERTI
        Standar Regulasi: Keputusan Kepala BSKAP No. 020 Tahun 2026
        Fase B (Kelas III & IV SD) - SDN Kiarapandak 01
        =======================================================
        SEMESTER 1:
        4.1. Membaca dan menghafalkan Q.S. Al-Hujurat/49:13 dengan tartil. (6 JP)
        4.2. Menganalisis pesan pokok keragaman sebagai fitrah untuk saling mengenal (ta'aruf). (6 JP)
        4.3. Meneladani makna Asmaulhusna (Al-Malik, Al-Aziz, Al-Quddus, As-Salam, Al-Mu'min). (9 JP)
        4.4. Membiasakan sikap toleransi dan tolong-menolong antarsesama teman. (6 JP)
        4.5. Mengidentifikasi tanda-tanda baligh menurut syariat fikih dan sains biologi. (9 JP)

        SEMESTER 2:
        4.6. Menjelaskan tata cara dan mempraktikkan mandi wajib sesuai rukun syariat. (9 JP)
        4.7. Menceritakan kisah keteladanan peristiwa hijrah Rasulullah SAW ke Madinah. (9 JP)
        4.8. Mengamalkan nilai persaudaraan kaum Muhajirin dan Ansar di sekolah. (6 JP)
        4.9. Menjaga kebersihan dan kelestarian lingkungan sekolah sebagai wujud syukur. (6 JP)
    """.trimIndent()
}

private fun generateSampleKktp(): String {
    return """
        KRITERIA KETERCAPAIAN TUJUAN PEMBELAJARAN (KKTP)
        Mata Pelajaran: PAI dan Budi Pekerti (Fase B Kelas IV)
        Pendekatan: Taksonomi SOLO & SK BSKAP No. 020 Tahun 2026
        =======================================================
        Materi: Memahami Tanda-Tanda Baligh dan Mandi Wajib
        
        Rubrik Ketercapaian (Taksonomi SOLO):
        - Level 1 (Unistruktural / 0-60%):
          Murid hanya dapat menyebutkan 1 tanda baligh tanpa memahami konsep mandiri dan belum memahami rukun mandi wajib. (Tindak Lanjut: Remedial terpadu dengan pendampingan guru).
        
        - Level 2 (Multistruktural / 61-75%):
          Murid dapat menyebutkan tanda fisik baligh dan urutan mandi wajib, namun belum memahami konsekuensi hukum mukallaf. (Tindak Lanjut: Latihan soal pemahaman konsep).
        
        - Level 3 (Relasional / 76-88% - TUNTAS SESUAI CP 020/2026):
          Murid mampu menghubungkan tanda baligh dengan kewajiban beribadah salat fardhu dan mempraktikkan mandi wajib secara sempurna. (Tindak Lanjut: Melanjutkan materi berikutnya).
        
        - Level 4 (Extended Abstract / 89-100% - PENGAYAAN):
          Murid mampu menguraikan hikmah syariat usia baligh serta membuat ajakan positif menjaga adab pergaulan islami bagi teman sebaya. (Tindak Lanjut: Menjadi tutor sebaya).
    """.trimIndent()
}
