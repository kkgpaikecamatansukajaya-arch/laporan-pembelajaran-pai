package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.*
import com.example.ui.AppViewModel
import com.example.ui.theme.*

// --- 1. PROFIL GURU ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilGuruScreen(viewModel: AppViewModel, teacher: TeacherProfile) {
    val context = LocalContext.current
    val school by viewModel.schoolData.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Ringkasan Kartu, 1: Formulir Biodata, 2: Tanda Tangan & Akun
    var showResetDialog by remember { mutableStateOf(false) }

    // State fields synced with teacher profile
    var fullName by remember(teacher) { mutableStateOf(teacher.fullName) }
    var nip by remember(teacher) { mutableStateOf(teacher.nip) }
    var nuptk by remember(teacher) { mutableStateOf(teacher.nuptk) }
    var certNo by remember(teacher) { mutableStateOf(teacher.certNo) }
    var rank by remember(teacher) { mutableStateOf(teacher.rank) }
    var education by remember(teacher) { mutableStateOf(teacher.education) }
    var email by remember(teacher) { mutableStateOf(teacher.email) }
    var phone by remember(teacher) { mutableStateOf(teacher.phone) }
    var employmentStatus by remember(teacher) { mutableStateOf(teacher.employmentStatus) }
    var subjectTaught by remember(teacher) { mutableStateOf(teacher.subjectTaught) }
    var workUnit by remember(teacher) { mutableStateOf(teacher.workUnit) }
    var additionalDuties by remember(teacher) { mutableStateOf(teacher.additionalDuties) }
    var birthPlaceDate by remember(teacher) { mutableStateOf(teacher.birthPlaceDate) }
    var isSignatureSigned by remember(teacher) { mutableStateOf(teacher.digitalSignature != null) }

    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportTeacherProfileToHtml(
                teacher = TeacherProfile(
                    fullName = fullName,
                    nip = nip,
                    nuptk = nuptk,
                    certNo = certNo,
                    rank = rank,
                    education = education,
                    digitalSignature = if (isSignatureSigned) "SIGNED" else null,
                    email = email,
                    phone = phone,
                    employmentStatus = employmentStatus,
                    subjectTaught = subjectTaught,
                    workUnit = workUnit,
                    additionalDuties = additionalDuties,
                    birthPlaceDate = birthPlaceDate
                ),
                school = school,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Biodata Profil Guru berhasil diekspor!", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(context, "Gagal mengekspor: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Profil Guru?") },
            text = { Text("Apakah Anda yakin ingin mengatur ulang data profil administrasi guru ke data standar bawaan?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetTeacherProfile()
                        showResetDialog = false
                        Toast.makeText(context, "Profil guru telah direset ke default", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Bar
        PrimaryTabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.White,
            contentColor = GreenPrimary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Ringkasan", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Sunting Biodata", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Tanda Tangan", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: RINGKASAN KARTU IDENTITAS GURU
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fullName.firstOrNull()?.toString() ?: "G",
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = GreenPrimary
                                    )
                                    Text("NIP. ${nip.ifEmpty { "-" }}", fontSize = 13.sp, color = Color.Gray)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GreenSecondary.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "Status: $employmentStatus",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GreenPrimary
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.LightGray.copy(alpha = 0.4f))

                            // Grid summary items
                            ProfileInfoRow(icon = Icons.Default.School, label = "Pendidikan", value = education.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.MenuBook, label = "Mata Pelajaran", value = subjectTaught.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.Apartment, label = "Unit Kerja", value = workUnit.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.WorkspacePremium, label = "Golongan / Pangkat", value = rank.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.ConfirmationNumber, label = "NUPTK / Sertifikat", value = "${nuptk.ifEmpty { "-" }} / ${certNo.ifEmpty { "-" }}")
                            ProfileInfoRow(icon = Icons.Default.TaskAlt, label = "Tugas Tambahan", value = additionalDuties.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.CalendarToday, label = "Tempat, Tgl Lahir", value = birthPlaceDate.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.Email, label = "Email Dinas", value = email.ifEmpty { "-" })
                            ProfileInfoRow(icon = Icons.Default.Phone, label = "WhatsApp / HP", value = phone.ifEmpty { "-" })
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportHtmlLauncher.launch("Biodata_Guru_${fullName.replace(" ", "_")}.html") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak / Ekspor")
                        }

                        Button(
                            onClick = { activeTab = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sunting Data")
                        }
                    }
                }

                1 -> {
                    // TAB 1: FORMULIR INPUT & SUNTING LENGKAP
                    Text("Informasi Pribadi & Kontak", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 15.sp)
                    
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap & Gelar") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = birthPlaceDate,
                        onValueChange = { birthPlaceDate = it },
                        label = { Text("Tempat & Tanggal Lahir") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Dinas") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("No. WhatsApp / HP") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true
                        )
                    }

                    Text("Status Kepegawaian & Sertifikasi", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))

                    // Employment status selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PPPK", "PNS", "Non-ASN / Honorer", "GTY (Yayasan)").forEach { status ->
                            FilterChip(
                                selected = employmentStatus == status,
                                onClick = { employmentStatus = status },
                                label = { Text(status) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nip,
                            onValueChange = { nip = it },
                            label = { Text("NIP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = nuptk,
                            onValueChange = { nuptk = it },
                            label = { Text("NUPTK") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = certNo,
                            onValueChange = { certNo = it },
                            label = { Text("No. Sertifikasi / NRG") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rank,
                            onValueChange = { rank = it },
                            label = { Text("Pangkat / Golongan") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("Penugasan Akademik", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))

                    OutlinedTextField(
                        value = education,
                        onValueChange = { education = it },
                        label = { Text("Riwayat Pendidikan Terakhir (mis. S1 PAI)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = subjectTaught,
                        onValueChange = { subjectTaught = it },
                        label = { Text("Mata Pelajaran yang Diampu") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = workUnit,
                        onValueChange = { workUnit = it },
                        label = { Text("Unit Kerja / Sekolah Induk") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = additionalDuties,
                        onValueChange = { additionalDuties = it },
                        label = { Text("Tugas Tambahan") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = null) },
                        singleLine = true
                    )

                    // Action buttons
                    Button(
                        onClick = {
                            viewModel.updateTeacherProfile(
                                TeacherProfile(
                                    fullName = fullName,
                                    nip = nip,
                                    nuptk = nuptk,
                                    certNo = certNo,
                                    rank = rank,
                                    education = education,
                                    digitalSignature = if (isSignatureSigned) "SIGNED" else null,
                                    email = email,
                                    phone = phone,
                                    employmentStatus = employmentStatus,
                                    subjectTaught = subjectTaught,
                                    workUnit = workUnit,
                                    additionalDuties = additionalDuties,
                                    birthPlaceDate = birthPlaceDate
                                )
                            )
                            Toast.makeText(context, "Profil administrasi guru berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            activeTab = 0
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Perubahan Profil", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset ke Data Bawaan")
                    }
                }

                2 -> {
                    // TAB 2: TANDA TANGAN DIGITAL & OTENTIKASI
                    Text("Tanda Tangan Digital Guru", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 15.sp)
                    Text("Tanda tangan digital ini digunakan untuk otentikasi berkas penilaian, jurnal harian, dan perangkat ajar PAI.", fontSize = 12.sp, color = Color.Gray)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isSignatureSigned) GreenPrimary else Color.LightGray)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isSignatureSigned) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("TTD_DIGITAL_${fullName.replace(" ", "_").uppercase()}.png", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("[ Terverifikasi & Tersimpan di Database SIM PAI ]", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Draw, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tanda tangan belum dibuat. Klik 'Buat / Perbarui TTD' di bawah.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isSignatureSigned = true
                                viewModel.updateTeacherProfile(
                                    TeacherProfile(
                                        fullName = fullName,
                                        nip = nip,
                                        nuptk = nuptk,
                                        certNo = certNo,
                                        rank = rank,
                                        education = education,
                                        digitalSignature = "SIGNED_${System.currentTimeMillis()}",
                                        email = email,
                                        phone = phone,
                                        employmentStatus = employmentStatus,
                                        subjectTaught = subjectTaught,
                                        workUnit = workUnit,
                                        additionalDuties = additionalDuties,
                                        birthPlaceDate = birthPlaceDate
                                    )
                                )
                                Toast.makeText(context, "Tanda tangan digital berhasil dibuat & disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buat TTD Digital", color = Color.White)
                        }

                        if (isSignatureSigned) {
                            OutlinedButton(
                                onClick = {
                                    isSignatureSigned = false
                                    viewModel.updateTeacherProfile(
                                        TeacherProfile(
                                            fullName = fullName,
                                            nip = nip,
                                            nuptk = nuptk,
                                            certNo = certNo,
                                            rank = rank,
                                            education = education,
                                            digitalSignature = null,
                                            email = email,
                                            phone = phone,
                                            employmentStatus = employmentStatus,
                                            subjectTaught = subjectTaught,
                                            workUnit = workUnit,
                                            additionalDuties = additionalDuties,
                                            birthPlaceDate = birthPlaceDate
                                        )
                                    )
                                    Toast.makeText(context, "Tanda tangan digital telah dihapus", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hapus TTD")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$label:",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50),
            modifier = Modifier.weight(1f)
        )
    }
}

// --- 2. DATA SEKOLAH ---
@Composable
fun DataSekolahScreen(viewModel: AppViewModel, school: SchoolData) {
    val context = LocalContext.current
    var schoolName by remember { mutableStateOf(school.schoolName) }
    var npsn by remember { mutableStateOf(school.npsn) }
    var address by remember { mutableStateOf(school.address) }
    var headmasterName by remember { mutableStateOf(school.headmasterName) }
    var academicYear by remember { mutableStateOf(school.academicYear) }
    var semester by remember { mutableStateOf(school.semester) }
    var curriculum by remember { mutableStateOf(school.curriculum) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profil & Administrasi Sekolah", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)

        OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("Nama Sekolah") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = npsn, onValueChange = { npsn = it }, label = { Text("NPSN") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Sekolah") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = headmasterName, onValueChange = { headmasterName = it }, label = { Text("Nama Kepala Sekolah") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = academicYear, onValueChange = { academicYear = it }, label = { Text("Tahun Pelajaran") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester (Ganjil/Genap)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = curriculum, onValueChange = { curriculum = it }, label = { Text("Kurikulum Aktif") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                viewModel.updateSchoolData(
                    SchoolData(
                        schoolName = schoolName,
                        npsn = npsn,
                        address = address,
                        headmasterName = headmasterName,
                        academicYear = academicYear,
                        semester = semester,
                        curriculum = curriculum
                    )
                )
                Toast.makeText(context, "Data sekolah berhasil disimpan!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text("Simpan Data Sekolah", color = Color.White)
        }
    }
}

// --- 3. PENILAIAN SISWA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenilaianSiswaScreen(
    viewModel: AppViewModel,
    assessments: List<AssessmentRecord>,
    students: List<Student>,
    classes: List<ClassRoom>
) {
    val context = LocalContext.current
    var showAddAssessmentDialog by remember { mutableStateOf(false) }

    // Form
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var selectedStudentId by remember { mutableStateOf<Int?>(null) }
    var assessmentType by remember { mutableStateOf("Formatif") } // Diagnostik, Formatif, Sumatif, Projek
    var topicName by remember { mutableStateOf("") }
    var scoreInput by remember { mutableStateOf("80") }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportAssessmentsToCsv(
                classId = selectedClassId,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Rekap nilai berhasil diekspor ke format CSV!", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(context, "Gagal mengekspor: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val exportRaporHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportRaporToHtml(
                classId = selectedClassId,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Laporan Rapor PAI berhasil dicetak ke HTML/PDF!", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(context, "Gagal mencetak rapor: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    LaunchedEffect(classes) {
        if (selectedClassId == null && classes.isNotEmpty()) {
            selectedClassId = classes.first().id
        }
    }

    val activeClass = classes.find { it.id == selectedClassId }
    val classStudents = if (selectedClassId != null) {
        students.filter { it.classId == selectedClassId }
    } else emptyList()

    LaunchedEffect(classStudents) {
        if (classStudents.isNotEmpty()) {
            selectedStudentId = classStudents.first().id
        }
    }

    // Auto generate description based on score (MANDATORY per prompt)
    val autoDescription = remember(scoreInput) {
        val sc = scoreInput.toIntOrNull() ?: 0
        when {
            sc >= 90 -> "Sangat baik dalam memahami materi dan mengaplikasikan nilai ajaran Islam."
            sc >= 75 -> "Baik dalam memahami materi, perlu sedikit bimbingan pada latihan praktik."
            else -> "Memerlukan pendampingan khusus dan bimbingan membaca Al-Qur'an secara terpadu."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Penilaian Peserta Didik", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
                    Text("Kelola nilai formatif, sumatif, diagnostik", fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { showAddAssessmentDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Text("Input Nilai", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Action panel for exporting report
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cName = activeClass?.name?.replace(" ", "_") ?: "Semua_Kelas"
                        exportCsvLauncher.launch("Rekap_Nilai_PAI_${cName}.csv")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    border = BorderStroke(1.dp, GreenPrimary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor CSV", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        val cName = activeClass?.name?.replace(" ", "_") ?: "Semua_Kelas"
                        exportRaporHtmlLauncher.launch("Rapor_PAI_${cName}.html")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    border = BorderStroke(1.dp, GreenPrimary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak Rapor", fontSize = 11.sp)
                }
            }
        }

        item {
            Text("Nilai Terinput", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 14.sp)
        }

        if (assessments.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada rekap nilai. Silakan tambahkan nilai siswa.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(assessments) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (record.score >= 75) GreenPrimary.copy(alpha = 0.1f) else Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${record.score}",
                                fontWeight = FontWeight.Bold,
                                color = if (record.score >= 75) GreenPrimary else Color.Red,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(record.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.background(GoldAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Text(record.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                }
                            }
                            Text("Topik: ${record.topic}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                            Text(record.description, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.deleteAssessment(record) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showAddAssessmentDialog) {
        AlertDialog(
            onDismissRequest = { showAddAssessmentDialog = false },
            title = { Text("Input Nilai Peserta Didik") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pilih Kelas:", fontSize = 11.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        classes.forEach { c ->
                            FilterChip(
                                selected = selectedClassId == c.id,
                                onClick = { selectedClassId = c.id },
                                label = { Text(c.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Pilih Siswa:", fontSize = 11.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        classStudents.forEach { s ->
                            FilterChip(
                                selected = selectedStudentId == s.id,
                                onClick = { selectedStudentId = s.id },
                                label = { Text(s.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Kategori Penilaian:", fontSize = 11.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Diagnostik", "Formatif", "Sumatif", "Projek").forEach { t ->
                            FilterChip(
                                selected = assessmentType == t,
                                onClick = { assessmentType = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(value = topicName, onValueChange = { topicName = it }, label = { Text("Kompetensi Dasar / Bab") }, placeholder = { Text("misal: Surah Al-Hujurat") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = scoreInput, onValueChange = { scoreInput = it }, label = { Text("Nilai Siswa (0 - 100)") }, modifier = Modifier.fillMaxWidth())

                    Text("Deskripsi Otomatis:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GreenPrimary)
                    Box(modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(8.dp)) {
                        Text(autoDescription, fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentClass = activeClass
                        val currentStudent = classStudents.find { it.id == selectedStudentId }
                        val scoreNum = scoreInput.toIntOrNull() ?: 0
                        if (currentClass != null && currentStudent != null && topicName.isNotBlank()) {
                            viewModel.addAssessment(
                                studentId = currentStudent.id,
                                studentName = currentStudent.name,
                                classId = currentClass.id,
                                type = assessmentType,
                                topic = topicName,
                                score = scoreNum,
                                description = autoDescription
                            )
                            showAddAssessmentDialog = false
                            topicName = ""
                            scoreInput = "80"
                            Toast.makeText(context, "Nilai berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAssessmentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- 4. KOKURIKULER SCREEN ---
@Composable
fun KokurikulerScreen(viewModel: AppViewModel) {
    val context = LocalContext.current

    val exportLaporanKokurikulerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportKokurikulerToHtml(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Laporan kegiatan kokurikuler berhasil dicetak ke HTML/PDF!", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(context, "Gagal mencetak: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Program Kokurikuler PAI", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
        Text("Aktivitas penunjang pemahaman materi PAI di luar kelas", fontSize = 12.sp, color = Color.Gray)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Projek Kunjungan Rumah Ibadah", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenPrimary)
                Text("Siswa diajak mengamati arsitektur masjid, musala, dan mengidentifikasi adab-adab bertamu.", fontSize = 12.sp, color = Color.Gray)
                Text("Jadwal: 25 Juli 2026", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Gerakan Sedekah Sampah Plastik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenPrimary)
                Text("Mengaplikasikan nilai iman kepada Allah lewat pelestarian alam dan kebersihan lingkungan.", fontSize = 12.sp, color = Color.Gray)
                Text("Jadwal: Rutin Setiap Jumat", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = {
                exportLaporanKokurikulerLauncher.launch("Laporan_Kokurikuler_PAI.html")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cetak Laporan Kegiatan", color = Color.White)
        }
    }
}

// --- 5. ROHIS & EKSTRAKURIKULER ---
@Composable
fun RohScreen(viewModel: AppViewModel) {
    // Falls back to RohisScreen
}

@Composable
fun RohisScreen(viewModel: AppViewModel, extracurriculars: List<Extracurricular>) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var mentor by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Ekstrakurikuler Rohani Islam (Rohis)", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
                    Text("Pembinaan karakter dan kajian keislaman siswa", fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Text("Tambah", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        if (extracurriculars.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada ekstrakurikuler terdaftar.", color = Color.Gray)
                }
            }
        } else {
            items(extracurriculars) { ext ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(ext.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenPrimary)
                            IconButton(onClick = { viewModel.deleteExtracurricular(ext) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Text("Pembimbing: ${ext.mentorName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("Jadwal: ${ext.schedule} • Anggota: ${ext.participantsCount} Siswa", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Text(ext.description, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Ekstrakurikuler Keagamaan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nama Ekstrakurikuler (Rohis/Tahfidz)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mentor, onValueChange = { mentor = it }, label = { Text("Nama Pembina / Mentor") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Jadwal (misal: Jumat 13:00)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi Singkat") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addExtracurricular(title, mentor, schedule, 15, desc)
                            showAddDialog = false
                            title = ""
                            mentor = ""
                            schedule = ""
                            desc = ""
                            Toast.makeText(context, "Ekstrakurikuler berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- 6. PROJEK P5 & PPRA ---
@Composable
fun ProjekP5Screen(viewModel: AppViewModel) {
    val context = LocalContext.current

    val exportP5Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportP5ModulToHtml(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Modul perencanaan Projek P5 + PPRA berhasil diunduh!", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Toast.makeText(context, "Gagal mengunduh: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Projek P5 & PPRA PAI", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
        Text("Rencana Projek Profil Pelajar Pancasila & Rahmatan lil Alamin", fontSize = 11.sp, color = Color.Gray)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema 1: Berakhlak Mulia bagi Sesama (Akhlak kepada Manusia)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GreenPrimary)
                Text("Sub-Tema: Indahnya Berbagi, Menghargai Perbedaan Suku", fontSize = 13.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                Text("Kelompok Sasaran: Kelas IV\nBentuk Projek: Bakti Sosial & Pembuatan infografis toleransi.", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema 2: Hidup Bersih Cermin Keimanan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GreenPrimary)
                Text("Sub-Tema: Pengolahan Sampah Organik Masjid Sekolah", fontSize = 13.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                Text("Kelompok Sasaran: Kelas V\nBentuk Projek: Kompos ramah lingkungan.", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Button(
            onClick = {
                exportP5Launcher.launch("Modul_Projek_P5_PPRA.html")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Unduh Modul Projek P5", color = Color.White)
        }
    }
}

// --- 7. ADMINISTRASI KEAGAMAAN ---
@Composable
fun AdministrasiKeagamaanScreen(viewModel: AppViewModel, activities: List<ReligiousActivity>) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-07-16") }
    var type by remember { mutableStateOf("Salat Berjamaah") }
    var desc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Administrasi Keagamaan Siswa", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
                    Text("Kelola salat, tadarus, pesantren kilat, zakat, kurban", fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Text("Catat", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        if (activities.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada pencatatan kegiatan keagamaan.", color = Color.Gray)
                }
            }
        } else {
            items(activities) { act ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(act.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                            IconButton(onClick = { viewModel.deleteReligiousActivity(act) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(GreenPrimary.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(act.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tanggal: ${act.date}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(act.description, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Catat Kegiatan Keagamaan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nama Kegiatan") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Tanggal") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Kategori:", fontSize = 11.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Salat Berjamaah", "Tadarus Al-Qur'an", "Kultum", "Zakat", "Kurban", "Ramadan").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 10.sp) })
                        }
                    }

                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi & Catatan Kegiatan") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addReligiousActivity(title, date, desc, type, 45)
                            showAddDialog = false
                            title = ""
                            desc = ""
                            Toast.makeText(context, "Kegiatan keagamaan dicatat offline!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- 8. BANK SOAL PAI ---
@Composable
fun BankSoalScreen(viewModel: AppViewModel, questions: List<Question>) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    var text by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    var answerKey by remember { mutableStateOf("A") }
    var diff by remember { mutableStateOf("Sedang") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Bank Soal PAI Digital", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
                    Text("Buat, simpan, acak soal berstandar HOTS", fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Text("Buat Soal", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        if (questions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada pertanyaan terdaftar di bank soal.", color = Color.Gray)
                }
            }
        } else {
            items(questions) { q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(if (q.difficulty == "HOTS") Color(0xFFFFEBEE) else Color(0xFFE8F5E9), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(q.difficulty, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (q.difficulty == "HOTS") Color.Red else GreenPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteQuestion(q) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Text(q.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                        Text("A. ${q.optionA}", fontSize = 12.sp, color = Color.Gray)
                        Text("B. ${q.optionB}", fontSize = 12.sp, color = Color.Gray)
                        Text("C. ${q.optionC}", fontSize = 12.sp, color = Color.Gray)
                        Text("D. ${q.optionD}", fontSize = 12.sp, color = Color.Gray)
                        HorizontalDivider()
                        Text("Kunci Jawaban: ${q.answerKey} • Pembahasan: ${q.explanation}", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Buat Soal PAI Baru") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Pertanyaan Soal") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Pilihan A") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("Pilihan B") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("Pilihan C") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("Pilihan D") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Kunci Jawaban:", fontSize = 11.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B", "C", "D").forEach { k ->
                            FilterChip(selected = answerKey == k, onClick = { answerKey = k }, label = { Text(k) })
                        }
                    }

                    Text("Tingkat Kesulitan:", fontSize = 11.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mudah", "Sedang", "HOTS").forEach { diffLevel ->
                            FilterChip(selected = diff == diffLevel, onClick = { diff = diffLevel }, label = { Text(diffLevel) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (text.isNotBlank() && a.isNotBlank()) {
                            viewModel.addQuestion(text, a, b, c, d, answerKey, diff, "Pilihan Ganda", "Jawaban yang benar adalah $answerKey")
                            showAddDialog = false
                            text = ""
                            a = ""
                            b = ""
                            c = ""
                            d = ""
                            Toast.makeText(context, "Soal berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- 9. ARSIP DIGITAL (SUPABASE STORAGE CLOUD) ---
@Composable
fun ArsipDigitalScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val archives by viewModel.digitalArchives.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploadingArchive.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentAppRole.collectAsStateWithLifecycle()

    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var docTitle by remember { mutableStateOf("") }
    var docCategory by remember { mutableStateOf("Sertifikat Pendidik") }
    var pendingDownloadItem by remember { mutableStateOf<DigitalArchiveItem?>(null) }
    var searchArchive by remember { mutableStateOf("") }

    val categories = listOf("Sertifikat Pendidik", "SK Mengajar", "Perangkat Ajar", "Dokumen KKG", "Lainnya")

    val uploadDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUploadUri = uri
            docTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "Dokumen_PAI"
        }
    }

    val downloadArchiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri: Uri? ->
        val target = pendingDownloadItem
        if (targetUri != null && target != null) {
            viewModel.downloadArchiveToUri(target, targetUri) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
        pendingDownloadItem = null
    }

    val backupDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Cadangan seluruh data SIM PAI berhasil disimpan!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mencadangkan: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val filteredArchives = remember(archives, searchArchive) {
        if (searchArchive.isBlank()) archives
        else archives.filter {
            it.title.contains(searchArchive, ignoreCase = true) ||
                it.category.contains(searchArchive, ignoreCase = true) ||
                it.fileName.contains(searchArchive, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Arsip Digital & Dokumen Cloud", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
        Text(
            "Penyimpanan berkas guru di Supabase Storage (bucket privat terenkripsi) & cadangan JSON.",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currentRole.canEditAcademicData) {
                Button(
                    onClick = { uploadDocLauncher.launch("*/*") },
                    enabled = !isUploading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("upload_archive_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isUploading) "Mengunggah..." else "Unggah ke Cloud", color = Color.White, fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    val dateNow = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                    backupDatabaseLauncher.launch("Backup_SIM_PAI_${dateNow}.json")
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("backup_json_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cadangkan Data", color = Color.White, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = searchArchive,
            onValueChange = { searchArchive = it },
            label = { Text("Cari dokumen cloud...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_archive_input"),
            shape = RoundedCornerShape(10.dp)
        )

        Text(
            "Berkas Tersimpan di Supabase Storage (${filteredArchives.size})",
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            fontSize = 14.sp
        )

        if (filteredArchives.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                    Text(
                        text = if (archives.isEmpty()) "Belum ada dokumen di cloud. Unggah sertifikat, SK, atau perangkat ajar Anda."
                        else "Tidak ada dokumen yang cocok dengan pencarian.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            filteredArchives.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${item.category} • ${item.fileName} (${item.fileSize})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                "Pengunggah: ${item.uploadedByName.ifBlank { "Guru PAI" }}",
                                fontSize = 10.sp,
                                color = GreenPrimary
                            )
                        }
                        IconButton(onClick = {
                            pendingDownloadItem = item
                            downloadArchiveLauncher.launch(item.fileName)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Unduh Dokumen", tint = GreenPrimary)
                        }
                        IconButton(onClick = {
                            viewModel.getArchiveSignedUrl(item) { url, err ->
                                if (url != null) {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }.onFailure {
                                        Toast.makeText(context, "Tautan siap: $url", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, err ?: "Gagal membuka dokumen", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Buka Tautan Cloud", tint = GoldAccent)
                        }
                        if (currentRole.canEditAcademicData) {
                            IconButton(onClick = {
                                viewModel.deleteDigitalArchive(item) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Dokumen", tint = Color(0xFFD32F2F))
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingUploadUri != null) {
        AlertDialog(
            onDismissRequest = { pendingUploadUri = null },
            title = { Text("Detail Dokumen Cloud", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Judul Dokumen") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { docCategory = cat }
                        ) {
                            RadioButton(selected = docCategory == cat, onClick = { docCategory = cat })
                            Text(cat, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUri = pendingUploadUri
                        pendingUploadUri = null
                        if (targetUri != null) {
                            viewModel.uploadDigitalArchiveFromUri(targetUri, docTitle, docCategory) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Unggah", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUploadUri = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- 10. LAPORAN ADMINISTRASI ---
@Composable
fun LaporanAdministrasiScreen(
    viewModel: AppViewModel,
    journals: List<TeachingJournal>,
    assessments: List<AssessmentRecord>,
    activities: List<ReligiousActivity>,
    extracurriculars: List<Extracurricular>,
    classes: List<ClassRoom>
) {
    val context = LocalContext.current

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Cadangan seluruh data SIM PAI berhasil disimpan!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengekspor cadangan: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val journalExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/msword")
    ) { uri ->
        uri?.let {
            viewModel.exportJournalReportToWord(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Rekap Jurnal Harian (Word) berhasil disimpan di perangkat!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengekspor rekap jurnal: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val assessmentExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri ->
        uri?.let {
            viewModel.exportAssessmentReportToExcel(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Rekap Penilaian Siswa (Excel/CSV) berhasil disimpan di perangkat!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengekspor rekap penilaian: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val activityExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        uri?.let {
            viewModel.exportActivityReportToPdf(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Laporan Kegiatan Keagamaan berhasil disimpan! Silakan buka file untuk mencetak ke PDF.", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengekspor laporan: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "Cadangan seluruh data SIM PAI berhasil dipulihkan!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal memulihkan cadangan: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Laporan & Rekap Administrasi Guru", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
        Text("Hasilkan rekap dokumen otomatis format PDF/Excel/Word atau kelola cadangan sistem", fontSize = 11.sp, color = Color.Gray)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rekap Mengajar Jurnal Harian", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("Mencakup rekapitulasi ${journals.size} jurnal mengajar guru PAI dan kehadiran siswa.", fontSize = 12.sp, color = Color.Gray)
                Button(
                    onClick = {
                        journalExportLauncher.launch("Rekap_Jurnal_Harian.doc")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unduh Rekap Jurnal (Word)", color = Color.White)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Laporan Penilaian Peserta Didik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("Mencakup rekapitulasi seluruh nilai diagnostik, formatif, sumatif, dan projek siswa.", fontSize = 12.sp, color = Color.Gray)
                Button(
                    onClick = {
                        assessmentExportLauncher.launch("Rekap_Nilai_Siswa.csv")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unduh Rekap Nilai (Excel)", color = Color.White)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Laporan Kokurikuler & Ekstrakurikuler", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("Rekap partisipasi siswa pada ${extracurriculars.size} Rohis/Tahfidz dan kegiatan keagamaan.", fontSize = 12.sp, color = Color.Gray)
                Button(
                    onClick = {
                        activityExportLauncher.launch("Laporan_Kegiatan_Ekstrakurikuler.html")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak Laporan Kegiatan (PDF / HTML)", color = Color.White)
                }
            }
        }

        // --- BACKUP & RESTORE SYSTEM CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pencadangan & Pemulihan Sistem (Backup)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenPrimary)
                }
                Text(
                    "Mengekspor seluruh database (Profil, Sekolah, Kelas, Siswa, Jurnal, Nilai, Bank Soal, Kegiatan Keagamaan, Ekstrakurikuler) ke file cadangan (.json), atau pulihkan data Anda dari file cadangan sebelumnya.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            val timestamp = sdf.format(java.util.Date())
                            backupExportLauncher.launch("BACKUP_SIM_PAI_$timestamp.json")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cadangkan", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            backupImportLauncher.launch(arrayOf("application/json"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pulihkan", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// --- 9. PENGADUAN / FEEDBACK ---
@Composable
fun PengaduanScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var submittedSuccessMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Pengaduan / Masukan", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)
        Text("Sampaikan keluhan atau masukan Anda kepada Pengurus KKG PAI Kecamatan Sukajaya.", fontSize = 12.sp, color = Color.Gray)

        if (submittedSuccessMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, GreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = submittedSuccessMessage ?: "",
                        fontSize = 12.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subjek Pengaduan / Usulan") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Isi Pengaduan / Masukan") },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            maxLines = 10
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (subject.isBlank() || body.isBlank()) {
                        Toast.makeText(context, "Mohon isi subjek dan isi pengaduan", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.logActivity("Mengirim pengaduan/masukan ke KKG PAI: $subject")
                        submittedSuccessMessage = "Terima kasih! Masukan '$subject' telah tercatat di sistem KKG PAI Kecamatan Sukajaya."
                        Toast.makeText(context, "Pengaduan berhasil dikirim & dicatat di sistem!", Toast.LENGTH_SHORT).show()
                        subject = ""
                        body = ""
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kirim ke Sistem KKG", color = Color.White, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    if (subject.isBlank() || body.isBlank()) {
                        Toast.makeText(context, "Mohon isi subjek dan isi pengaduan", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.logActivity("Mengirim email pengaduan KKG PAI: $subject")
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:kkgpaikecamatansukajaya@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Kirim Pengaduan via Email..."))
                        } catch (_: Exception) {
                            submittedSuccessMessage = "Masukan '$subject' telah disimpan langsung ke log KKG PAI."
                            Toast.makeText(context, "Masukan dicatat langsung ke sistem KKG PAI", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Via Email", color = GreenPrimary, fontSize = 12.sp)
            }
        }
    }
}

