package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ClassRoom
import com.example.data.models.Student
import com.example.data.models.TeachingJournal
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministrasiPembelajaranScreen(
    viewModel: AppViewModel,
    journals: List<TeachingJournal>,
    classes: List<ClassRoom>,
    students: List<Student>
) {
    val context = LocalContext.current
    var showAddJournalDialog by remember { mutableStateOf(false) }
    var selectedJournalForDetail by remember { mutableStateOf<TeachingJournal?>(null) }

    // Form states
    var journalDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var journalTopic by remember { mutableStateOf("") }
    var journalActivity by remember { mutableStateOf("") }
    var teacherPresent by remember { mutableStateOf(true) }
    
    // GPS & Photo states
    var simulatedGps by remember { mutableStateOf<String?>(null) }
    var simulatedPhoto by remember { mutableStateOf<String?>(null) }

    // Map to track presence status of each student for selected class
    // studentId -> presence ("Hadir", "Sakit", "Izin", "Alpa")
    val studentPresenceMap = remember { mutableStateMapOf<Int, String>() }

    // Default class ID
    LaunchedEffect(classes) {
        if (selectedClassId == null && classes.isNotEmpty()) {
            selectedClassId = classes.first().id
        }
    }

    // Populate presence list with default "Hadir" when class changes
    LaunchedEffect(selectedClassId, students) {
        studentPresenceMap.clear()
        if (selectedClassId != null) {
            students.filter { it.classId == selectedClassId }.forEach { s ->
                studentPresenceMap[s.id] = "Hadir"
            }
        }
    }

    val selectedClass = classes.find { it.id == selectedClassId }
    val selectedClassStudents = if (selectedClassId != null) {
        students.filter { it.classId == selectedClassId }
    } else {
        emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Title Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Jurnal & Agenda Mengajar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Text(
                        text = "Catatan administrasi pembelajaran harian",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Button(
                    onClick = { showAddJournalDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Isi Jurnal", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // 2. Attendance Summary / Quick indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${journals.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        Text(text = "Total Jurnal", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "100%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        Text(text = "Presensi Guru", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "97.4%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                        Text(text = "Rata Presensi Siswa", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // 3. Jurnal list header
        item {
            Text(
                text = "Riwayat Jurnal Harian",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        }

        // 4. Jurnal Entries List cards
        if (journals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada jurnal mengajar harian. Silakan isi jurnal baru.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(journals) { jrn ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedJournalForDetail = jrn },
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
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = GreenPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = jrn.date, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .background(GreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(jrn.className, color = GreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = jrn.topic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = jrn.activity,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = jrn.locationGps ?: "Lokasi: Di Luar Jangkauan",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            if (jrn.photoPath != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldAccent)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Foto Terlampir", fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Add Journal Entry Dialog
    if (showAddJournalDialog) {
        AlertDialog(
            onDismissRequest = { showAddJournalDialog = false },
            title = { Text("Isi Jurnal Mengajar Harian") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date picker simulation
                    OutlinedTextField(
                        value = journalDate,
                        onValueChange = { journalDate = it },
                        label = { Text("Tanggal (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Class dropdown selection simulation
                    Text("Pilih Kelas:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        classes.forEach { cls ->
                            val isSelected = cls.id == selectedClassId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedClassId = cls.id },
                                label = { Text(cls.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = journalTopic,
                        onValueChange = { journalTopic = it },
                        label = { Text("Materi Pembelajaran / Kompetensi") },
                        placeholder = { Text("misal: Bab 2: Menyambut Usia Baligh") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = journalActivity,
                        onValueChange = { journalActivity = it },
                        label = { Text("Aktivitas Pembelajaran / Jurnal") },
                        placeholder = { Text("misal: Berdiskusi dalam kelompok, mengamati infografis tanda-tanda baligh") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        singleLine = false
                    )

                    // GPS & Camera Simulation Buttons (MANDATORY per user prompt)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // GPS grabber
                        Button(
                            onClick = {
                                simulatedGps = "-6.5971, 106.7996 (Bogor, Indonesia)"
                                Toast.makeText(context, "GPS Berhasil Mengambil Lokasi!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (simulatedGps != null) Color(0xFFE8F5E9) else Color.LightGray.copy(alpha = 0.2f),
                                contentColor = if (simulatedGps != null) GreenPrimary else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (simulatedGps != null) "GPS Aktif" else "Ambil GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Camera grabber
                        Button(
                            onClick = {
                                simulatedPhoto = "img_journal_${System.currentTimeMillis()}.jpg"
                                Toast.makeText(context, "Foto Dokumentasi Diambil!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (simulatedPhoto != null) Color(0xFFFFF9C4) else Color.LightGray.copy(alpha = 0.2f),
                                contentColor = if (simulatedPhoto != null) Color(0xFFF57F17) else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (simulatedPhoto != null) "Foto Tersimpan" else "Ambil Foto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider()

                    // Student Attendance checklist
                    Text("Presensi Kehadiran Siswa:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenPrimary)
                    
                    if (selectedClassStudents.isEmpty()) {
                        Text("Belum ada siswa terdaftar di kelas ini.", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        selectedClassStudents.forEach { student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(student.name, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Hadir", "Sakit", "Izin", "Alpa").forEach { status ->
                                        val isCurrent = studentPresenceMap[student.id] == status
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = { studentPresenceMap[student.id] = status },
                                            label = { Text(status, fontSize = 9.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = when (status) {
                                                    "Hadir" -> GreenPrimary
                                                    "Sakit" -> Color(0xFFFFF176)
                                                    "Izin" -> Color(0xFF64B5F6)
                                                    else -> Color(0xFFE57373)
                                                },
                                                selectedLabelColor = if (status == "Sakit") Color.Black else Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentClassId = selectedClassId
                        val currentClass = selectedClass
                        if (currentClassId != null && currentClass != null && journalTopic.isNotBlank()) {
                            // Compile student presence mapping as simple String/JSON presentation
                            val presenceBuilder = StringBuilder()
                            selectedClassStudents.forEach { student ->
                                val status = studentPresenceMap[student.id] ?: "Hadir"
                                presenceBuilder.append("${student.name}: $status\n")
                            }

                            viewModel.addJournal(
                                date = journalDate,
                                classId = currentClassId,
                                className = currentClass.name,
                                topic = journalTopic,
                                activity = journalActivity,
                                teacherPresent = teacherPresent,
                                presenceJson = presenceBuilder.toString(),
                                location = simulatedGps ?: "Sekolah (-6.5971, 106.7996)",
                                photo = simulatedPhoto
                            )

                            showAddJournalDialog = false
                            journalTopic = ""
                            journalActivity = ""
                            simulatedGps = null
                            simulatedPhoto = null
                            Toast.makeText(context, "Jurnal Mengajar disimpan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddJournalDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Journal detail viewer
    if (selectedJournalForDetail != null) {
        val jrn = selectedJournalForDetail!!
        AlertDialog(
            onDismissRequest = { selectedJournalForDetail = null },
            title = { Text("Detail Jurnal Mengajar") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tanggal: ${jrn.date} • Kelas: ${jrn.className}", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 13.sp)
                    HorizontalDivider()
                    Text("Topik / Materi:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(jrn.topic, fontSize = 13.sp, color = TextDark)
                    
                    Text("Aktivitas Pembelajaran:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(jrn.activity, fontSize = 13.sp, color = TextDark)

                    Text("Lokasi Pembelajaran:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(jrn.locationGps ?: "Tidak Terdaftar", fontSize = 12.sp, color = TextDark)
                    }

                    if (jrn.photoPath != null) {
                        Text("Dokumentasi Kegiatan:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Photo, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
                                Text("Foto: ${jrn.photoPath}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Absensi Peserta Didik:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenPrimary)
                    Text(jrn.studentPresenceJson, fontSize = 12.sp, lineHeight = 18.sp, color = TextDark)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedJournalForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Tutup", color = Color.White)
                }
            },
            dismissButton = {
                IconButton(onClick = {
                    viewModel.deleteJournal(jrn)
                    selectedJournalForDetail = null
                    Toast.makeText(context, "Jurnal Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Jurnal", tint = Color.Gray)
                }
            }
        )
    }
}
