package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.*
import com.example.ui.AppViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    teacher: TeacherProfile,
    school: SchoolData,
    classes: List<ClassRoom>,
    students: List<Student>,
    documents: List<MerdekaDocument>,
    journals: List<TeachingJournal>,
    assessments: List<AssessmentRecord>,
    activities: List<ReligiousActivity>,
    onNavigateToMenu: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // State collections
    val sukajayaSchools by viewModel.filteredSukajayaSchools.collectAsState()
    val allSukajayaSchools by viewModel.sukajayaSchools.collectAsState()
    val sukajayaTeachers by viewModel.filteredSukajayaTeachers.collectAsState()
    val allSukajayaTeachers by viewModel.sukajayaTeachers.collectAsState()
    val sukajayaSummary by viewModel.sukajayaSummary.collectAsState()
    val searchQuery by viewModel.schoolSearchQuery.collectAsState()
    val statusFilter by viewModel.schoolStatusFilter.collectAsState()
    val desaFilter by viewModel.selectedDesaFilter.collectAsState()
    val kepegawaianFilter by viewModel.selectedKepegawaianFilter.collectAsState()

    // Dashboard view mode: 0 = Monitoring KKG Kec. Sukajaya, 1 = Administrasi Saya
    var selectedDashboardTab by remember { mutableIntStateOf(0) }
    var selectedKkgSubTab by remember { mutableIntStateOf(0) } // 0 = Per Satminkal, 1 = Rekap 33 Guru PAI
    var selectedSchoolForDetail by remember { mutableStateOf<SukajayaSchoolProgress?>(null) }
    var selectedTeacherForDetail by remember { mutableStateOf<SukajayaTeacherItem?>(null) }
    var showKkgPengurusDialog by remember { mutableStateOf(false) }

    val villageList = remember {
        listOf(
            "Semua Desa",
            "Kiarapandak",
            "Sukajaya",
            "Pasir Madang",
            "Cileuksa",
            "Harkatjaya",
            "Jayaraharja",
            "Urug",
            "Sipayung",
            "Cisarua",
            "Sukamulih"
        )
    }

    val kepegawaianList = remember {
        listOf(
            "Semua Status",
            "PPPK 2024",
            "PPPK 2026",
            "PPPK 2025",
            "PNS DEPAG",
            "PNS DINAS",
            "HONORER"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. Mode Switcher (Tab Utama Dashboard) ---
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_mode_switcher"),
                shape = RoundedCornerShape(16.dp),
                color = GreenPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tab 0: Monitoring KKG Sukajaya
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedDashboardTab = 0 },
                        color = if (selectedDashboardTab == 0) GreenPrimary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (selectedDashboardTab == 0) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = if (selectedDashboardTab == 0) GoldLight else GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KKG Kec. Sukajaya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDashboardTab == 0) Color.White else GreenPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (selectedDashboardTab == 0) GoldAccent else GreenPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${allSukajayaSchools.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDashboardTab == 0) Color.White else GreenPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Tab 1: Administrasi Saya
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedDashboardTab = 1 },
                        color = if (selectedDashboardTab == 1) GreenPrimary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (selectedDashboardTab == 1) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedDashboardTab == 1) GoldLight else GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Administrasi Saya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDashboardTab == 1) Color.White else GreenPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // VIEW 0: MONITORING KKG PAI KECAMATAN SUKAJAYA (Kebutuhan Utama Pengguna)
        // =========================================================================
        if (selectedDashboardTab == 0) {
            // 1. Sukajaya KKG Header Hero Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sukajaya_hero_banner"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSecondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = size.minDimension * 0.45f,
                                    center = Offset(size.width * 0.95f, size.height * 0.85f)
                                )
                                drawCircle(
                                    color = GoldAccent.copy(alpha = 0.15f),
                                    radius = size.minDimension * 0.3f,
                                    center = Offset(size.width * 0.85f, size.height * 0.2f)
                                )
                            }
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "KKG PAI KEC. SUKAJAYA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "TA 2026/2027 Ganjil",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Monitoring Administrasi Guru PAI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Wilayah Binaan Kab. Bogor • Standar Kurikulum Merdeka (CP 020/2026)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Overall Progress Bar of the District
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rata-rata Kelengkapan Administrasi Kec.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${sukajayaSummary.averageProgressPercent}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(sukajayaSummary.averageProgressPercent / 100f)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(GoldAccent, GoldLight)
                                                )
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Validasi KKG: ${sukajayaSummary.completeSchoolsCount} dari ${sukajayaSummary.totalSchools} SD Selesai",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                TextButton(
                                    onClick = { showKkgPengurusDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = GoldLight),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Struktur KKG >",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Ringkasan Data Guru PAI (4 Kartu KPI Utama)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ringkasan Data Guru PAI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Kec. Sukajaya",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SukajayaKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Guru PAI",
                            value = "${sukajayaSummary.totalTeachers}",
                            subtext = "Aktif di SIAGA",
                            icon = Icons.Default.People,
                            accentColor = GreenPrimary
                        )
                        SukajayaKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Sekolah Binaan",
                            value = "${sukajayaSummary.totalSchools}",
                            subtext = "10 Desa Se-Kec.",
                            icon = Icons.Default.School,
                            accentColor = Color(0xFF0284C7) // Sky 600
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SukajayaKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Tersertifikasi",
                            value = "${sukajayaSummary.certifiedCount}",
                            subtext = "94% Sergu PAI",
                            icon = Icons.Default.WorkspacePremium,
                            accentColor = GoldAccent
                        )
                        SukajayaKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Belum Sertifikasi",
                            value = "${sukajayaSummary.uncertifiedCount}",
                            subtext = "6% Non-Serti (PPG)",
                            icon = Icons.Default.HourglassTop,
                            accentColor = Color(0xFF0D9488) // Teal 600
                        )
                    }
                }
            }

            // 3. Distribusi Status Kepegawaian & Kualifikasi Guru PAI Sukajaya
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distribusi Kepegawaian Guru PAI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Surface(
                                color = GreenPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${sukajayaSummary.totalTeachers} Guru",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Visual Proportional Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(sukajayaSummary.pppkCount.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(GreenPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(sukajayaSummary.pnsCount.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(GoldAccent)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(sukajayaSummary.honorerCount.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(Color(0xFF0284C7))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            KepegawaianLegendItem(
                                label = "PPPK",
                                count = "${sukajayaSummary.pppkCount} Guru",
                                percent = "85%",
                                color = GreenPrimary
                            )
                            KepegawaianLegendItem(
                                label = "PNS",
                                count = "${sukajayaSummary.pnsCount} Guru",
                                percent = "6%",
                                color = GoldAccent
                            )
                            KepegawaianLegendItem(
                                label = "Honorer",
                                count = "${sukajayaSummary.honorerCount} Guru",
                                percent = "9%",
                                color = Color(0xFF0284C7)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rincian PPPK & Kualifikasi:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "2024: 15 • 2026: 12 • 2025: 1",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Sub-Tab Switcher: Per Satminkal (Sekolah Induk) vs Rekap 33 Guru PAI
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedKkgSubTab = 0 },
                            color = if (selectedKkgSubTab == 0) GreenPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = if (selectedKkgSubTab == 0) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (selectedKkgSubTab == 0) Color.White else GreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Per Satminkal (${allSukajayaSchools.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedKkgSubTab == 0) Color.White else TextDark
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedKkgSubTab = 1 },
                            color = if (selectedKkgSubTab == 1) GreenPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = if (selectedKkgSubTab == 1) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (selectedKkgSubTab == 1) Color.White else GreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Data 33 Guru PAI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedKkgSubTab == 1) Color.White else TextDark
                                )
                            }
                        }
                    }
                }
            }

            // 5. Status Progres Filter Cards
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedKkgSubTab == 0) "Status Berkas Satminkal" else "Status Berkas Guru PAI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Kurikulum Merdeka CP 020",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Filter Status Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminStatusCard(
                            modifier = Modifier.weight(1f),
                            label = "Lengkap",
                            count = "${sukajayaSummary.completeSchoolsCount}",
                            subtext = "100% Berkas",
                            color = GreenPrimary,
                            isSelected = statusFilter == SchoolAdminStatus.LENGKAP,
                            onClick = {
                                if (statusFilter == SchoolAdminStatus.LENGKAP) {
                                    viewModel.setSchoolStatusFilter(null)
                                } else {
                                    viewModel.setSchoolStatusFilter(SchoolAdminStatus.LENGKAP)
                                }
                            }
                        )
                        AdminStatusCard(
                            modifier = Modifier.weight(1f),
                            label = "Dalam Proses",
                            count = "${sukajayaSummary.inProgressSchoolsCount}",
                            subtext = "70 - 95%",
                            color = GoldAccent,
                            isSelected = statusFilter == SchoolAdminStatus.DALAM_PROSES,
                            onClick = {
                                if (statusFilter == SchoolAdminStatus.DALAM_PROSES) {
                                    viewModel.setSchoolStatusFilter(null)
                                } else {
                                    viewModel.setSchoolStatusFilter(SchoolAdminStatus.DALAM_PROSES)
                                }
                            }
                        )
                        AdminStatusCard(
                            modifier = Modifier.weight(1f),
                            label = "Perlu Bantuan",
                            count = "${sukajayaSummary.needAssistanceSchoolsCount}",
                            subtext = "< 70%",
                            color = Color(0xFFDC2626),
                            isSelected = statusFilter == SchoolAdminStatus.PERLU_PENDAMPINGAN,
                            onClick = {
                                if (statusFilter == SchoolAdminStatus.PERLU_PENDAMPINGAN) {
                                    viewModel.setSchoolStatusFilter(null)
                                } else {
                                    viewModel.setSchoolStatusFilter(SchoolAdminStatus.PERLU_PENDAMPINGAN)
                                }
                            }
                        )
                    }
                }
            }

            // 6. Search Bar, Desa Filter & Status Kepegawaian Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search Text Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSchoolSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_search_field"),
                        placeholder = {
                            Text(
                                if (selectedKkgSubTab == 0) "Cari Satminkal, desa, nama guru, atau NPSN..." else "Cari nama guru, Satminkal, Akun SIAGA, NUPTK...",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSchoolSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Village Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(villageList) { desa ->
                            val isSelected = (desa == "Semua Desa" && desaFilter == null) || (desa == desaFilter)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (desa == "Semua Desa") {
                                        viewModel.setSelectedDesaFilter(null)
                                    } else {
                                        viewModel.setSelectedDesaFilter(if (isSelected) null else desa)
                                    }
                                },
                                label = {
                                    Text(
                                        text = desa,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = TextDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) GreenPrimary else Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Kepegawaian Filter Chips (if in Guru PAI sub-tab)
                    if (selectedKkgSubTab == 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(kepegawaianList) { kep ->
                                val isSelected = (kep == "Semua Status" && kepegawaianFilter == null) || (kep == kepegawaianFilter)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (kep == "Semua Status") {
                                            viewModel.setSelectedKepegawaianFilter(null)
                                        } else {
                                            viewModel.setSelectedKepegawaianFilter(if (isSelected) null else kep)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = kep,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = TextDark
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) GoldAccent else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Section Header & Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedKkgSubTab == 0)
                            "Daftar Satminkal SD (${sukajayaSchools.size} Sekolah)"
                        else
                            "Daftar Guru PAI Sesuai Satminkal (${sukajayaTeachers.size} Guru)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    if (statusFilter != null || desaFilter != null || kepegawaianFilter != null || searchQuery.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.setSchoolSearchQuery("")
                                viewModel.setSchoolStatusFilter(null)
                                viewModel.setSelectedDesaFilter(null)
                                viewModel.setSelectedKepegawaianFilter(null)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset Filter", fontSize = 11.sp, color = GreenPrimary)
                        }
                    }
                }
            }

            // 8. List Items: Sub-Tab 0 = Satminkal, Sub-Tab 1 = 33 Guru PAI
            if (selectedKkgSubTab == 0) {
                if (sukajayaSchools.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tidak ada sekolah satminkal yang cocok",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Silakan ubah kata kunci pencarian atau reset filter wilayah.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(sukajayaSchools, key = { it.id }) { schoolItem ->
                        SukajayaSchoolCard(
                            school = schoolItem,
                            onViewDetail = { selectedSchoolForDetail = schoolItem },
                            onToggleVerify = {
                                viewModel.toggleSchoolVerification(schoolItem.id)
                                Toast.makeText(
                                    context,
                                    if (!schoolItem.verifiedByKkg) "Administrasi ${schoolItem.schoolName} berhasil diverifikasi KKG!" else "Verifikasi ${schoolItem.schoolName} dibatalkan.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            } else {
                // Sub-Tab 1: List 33 Guru PAI Sesuai Satminkal
                if (sukajayaTeachers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tidak ada data guru yang cocok",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "Silakan sesuaikan kata kunci pencarian atau filter status kepegawaian.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(sukajayaTeachers, key = { it.no }) { teacherItem ->
                        SukajayaTeacherCard(
                            teacher = teacherItem,
                            onViewDetail = { selectedTeacherForDetail = teacherItem },
                            onToggleVerify = {
                                viewModel.toggleTeacherVerification(teacherItem.no)
                                Toast.makeText(
                                    context,
                                    if (!teacherItem.verifiedByKkg) "Guru ${teacherItem.name} terverifikasi KKG!" else "Verifikasi guru ${teacherItem.name} dibatalkan.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // VIEW 1: ADMINISTRASI SAYA (GURU PAI PENGGUNA - SDN KIARAPANDAK 01)
        // =========================================================================
        if (selectedDashboardTab == 1) {
            // Teacher Profile Hero
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_welcome_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSecondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = size.minDimension * 0.4f,
                                    center = Offset(size.width * 0.95f, size.height * 0.95f)
                                )
                                drawCircle(
                                    color = GoldAccent.copy(alpha = 0.12f),
                                    radius = size.minDimension * 0.25f,
                                    center = Offset(size.width * 0.85f, size.height * 0.15f)
                                )
                            }
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "Selamat datang,",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = teacher.fullName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${school.schoolName} • TA ${school.academicYear}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SEMESTER",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "${school.semester} (Aktif)",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )

                                Column {
                                    Text(
                                        text = "STATUS",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(GoldLight)
                                        )
                                        Text(
                                            text = "${teacher.employmentStatus} • Sertifikasi",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Alerts
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifikasi Administrasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(GoldAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = " Belum Lengkap ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlertItem(
                            title = "Jurnal Mengajar Belum Lengkap",
                            desc = "Jurnal mengajar harian hari ini belum diisi.",
                            actionText = "Isi Sekarang",
                            icon = Icons.Default.Assignment,
                            color = Color(0xFFFFB300),
                            onClick = { onNavigateToMenu("administrasi") }
                        )
                        AlertItem(
                            title = "Modul Ajar Kurang",
                            desc = "Segera buat Modul Ajar '.",
                            actionText = "Buat dengan AI",
                            icon = Icons.Default.AutoAwesome,
                            color = GreenPrimary,
                            onClick = { onNavigateToMenu("perangkat") }
                        )
                    }
                }
            }

            // Stats Grid
            item {
                Column {
                    Text(
                        text = "Rekapitulasi Data Sekolah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Kelas Diampu",
                            value = "${classes.size}",
                            icon = Icons.Default.Class,
                            color = GreenPrimary,
                            onClick = { onNavigateToMenu("kelas_siswa") }
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Peserta Didik",
                            value = "${students.size}",
                            icon = Icons.Default.People,
                            color = GoldAccent,
                            onClick = { onNavigateToMenu("kelas_siswa") }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Perangkat",
                            value = "${documents.size}",
                            icon = Icons.Default.MenuBook,
                            color = GreenSecondary,
                            onClick = { onNavigateToMenu("perangkat") }
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Kehadiran Siswa",
                            value = if (journals.isEmpty()) "98%" else "96.5%",
                            icon = Icons.Default.TaskAlt,
                            color = Color(0xFF00796B),
                            onClick = { onNavigateToMenu("absensi") }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Presensi Siswa",
                            value = "Input Harian",
                            icon = Icons.Default.FactCheck,
                            color = Color(0xFF10B981),
                            onClick = { onNavigateToMenu("absensi") }
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Hafalan Surat",
                            value = "${viewModel.surahCatalog.size} Surat",
                            icon = Icons.Default.AutoStories,
                            color = GoldAccent,
                            onClick = { onNavigateToMenu("hafalan") }
                        )
                    }
                }
            }

            // Capaian Pembelajaran Canvas Graph
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Grafik Capaian Pembelajaran PAI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Rata-rata nilai formatif & sumatif per kelas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val classesCount = 3
                                val names = listOf("Kls 1A", "Kls 4B", "Kls 5A")
                                val scores = listOf(84f, 78f, 88f)

                                val paddingBottom = 30f
                                val paddingLeft = 50f
                                val chartWidth = size.width - paddingLeft - 10f
                                val chartHeight = size.height - paddingBottom

                                val levels = 4
                                for (i in 0..levels) {
                                    val y = chartHeight - (chartHeight / levels * i)
                                    drawLine(
                                        color = Color(0xFFF1F5F9),
                                        start = Offset(paddingLeft, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        (i * 25).toString(),
                                        paddingLeft - 10f,
                                        y + 8f,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 22f
                                            textAlign = android.graphics.Paint.Align.RIGHT
                                        }
                                    )
                                }

                                val barWidth = 45f
                                val spacing = (chartWidth - (barWidth * classesCount)) / (classesCount + 1)

                                for (index in 0 until classesCount) {
                                    val x = paddingLeft + spacing + (index * (barWidth + spacing))
                                    val scoreHeight = (scores[index] / 100f) * chartHeight
                                    val y = chartHeight - scoreHeight

                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = if (index == 1) listOf(GoldAccent, GoldLight) else listOf(GreenPrimary, GreenPrimary.copy(alpha = 0.7f))
                                        ),
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, scoreHeight),
                                        style = androidx.compose.ui.graphics.drawscope.Fill
                                    )

                                    drawContext.canvas.nativeCanvas.drawText(
                                        "${scores[index].toInt()}",
                                        x + barWidth / 2f,
                                        y - 8f,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.DKGRAY
                                            textSize = 20f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            isFakeBoldText = true
                                        }
                                    )

                                    drawContext.canvas.nativeCanvas.drawText(
                                        names[index],
                                        x + barWidth / 2f,
                                        size.height - 4f,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.BLACK
                                            textSize = 20f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Capaian Pembelajaran (CP 020/2026)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark
                            )
                            Text(
                                text = "5 Elemen PAI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenPrimary
                            )
                        }
                        Text(
                            text = "Pedoman: SK Kepala BSKAP No. 020 Tahun 2026 • 5 Dimensi Cinta",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ProgressItem(title = "Al-Qur'an dan Hadis", progress = 0.85f, color = GreenPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressItem(title = "Akidah & Asmaul Husna", progress = 0.70f, color = GreenPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressItem(title = "Akhlak Mulia (5 Dimensi Cinta)", progress = 0.90f, color = GreenPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressItem(title = "Fikih Ibadah & Taharah", progress = 0.80f, color = GoldAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressItem(title = "Sejarah Peradaban Islam", progress = 0.65f, color = GreenSecondary)
                    }
                }
            }

            // Schedule & Calendar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jadwal Mengajar Hari Ini",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = GreenPrimary
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ScheduleItem(time = "07:30 - 09:15", className = "Kelas 4-B", subject = "Bab 2: Menyambut Usia Baligh", isCompleted = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScheduleItem(time = "09:30 - 11:15", className = "Kelas 1-A", subject = "Rukun Iman yang Kedua", isCompleted = false)
                    }
                }
            }

            // Quick AI Banners
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMenu("asisten_ai") }
                        .testTag("ai_quick_assistant"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tanya Asisten AI Guru",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "\"Buatkan soal HOTS bab Adab...\"",
                                fontSize = 10.sp,
                                color = Color(0xFFB45309),
                                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.5.dp, Color(0xFFFCD34D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMenu("studio_media") }
                        .testTag("studio_media_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCameraBack,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Studio Media AI (Veo 3 & Gemini 3.1)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = GoldAccent,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("BARU", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text(
                                text = "Teks ke Video (16:9 / 9:16), Animasi Foto, & Edit Gambar",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .border(1.5.dp, GreenPrimary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Modal Dialog Detail Progres Sekolah ---
    selectedSchoolForDetail?.let { activeSchool ->
        SchoolDetailAdminDialog(
            school = activeSchool,
            onDismiss = { selectedSchoolForDetail = null },
            onToggleChecklist = { itemType, isDone ->
                viewModel.updateSchoolChecklistItem(activeSchool.id, itemType, isDone)
                // Refresh local dialog preview
                selectedSchoolForDetail = sukajayaSchools.find { it.id == activeSchool.id }
            },
            onToggleVerify = {
                viewModel.toggleSchoolVerification(activeSchool.id)
                selectedSchoolForDetail = sukajayaSchools.find { it.id == activeSchool.id }
                Toast.makeText(
                    context,
                    if (!activeSchool.verifiedByKkg) "Administrasi ${activeSchool.schoolName} disahkan KKG." else "Status verifikasi dicabut.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCopySummary = {
                val summaryText = """
                    REKAP PROGRES ADMINISTRASI GURU PAI
                    Sekolah: ${activeSchool.schoolName} (${activeSchool.npsn})
                    Desa: ${activeSchool.desa}, Kec. Sukajaya
                    Guru PAI: ${activeSchool.teacherName} (${activeSchool.employmentStatus})
                    Progres: ${activeSchool.progressPercent}% (${activeSchool.status.label})
                    Status Verifikasi: ${if (activeSchool.verifiedByKkg) "TERVERIFIKASI KKG" else "BELUM VERIFIKASI"}
                    Rincian:
                    - CP & ATP: ${if (activeSchool.cpAtpDone) "Lengkap" else "Belum"}
                    - Modul Ajar: ${if (activeSchool.modulAjarDone) "Lengkap" else "Belum"}
                    - Prota & Promes: ${if (activeSchool.protaPromesDone) "Lengkap" else "Belum"}
                    - KKTP: ${if (activeSchool.kktpDone) "Lengkap" else "Belum"}
                    - Jurnal Mengajar: ${if (activeSchool.jurnalDone) "Lengkap" else "Belum"}
                    - Asesmen/Nilai: ${if (activeSchool.asesmenDone) "Lengkap" else "Belum"}
                    - Ekstra/Pembiasaan: ${if (activeSchool.ekstraDone) "Lengkap" else "Belum"}
                    Catatan: ${activeSchool.notes}
                """.trimIndent()
                clipboardManager.setText(AnnotatedString(summaryText))
                Toast.makeText(context, "Ringkasan berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Modal Dialog Detail Progres Guru PAI ---
    selectedTeacherForDetail?.let { activeTeacher ->
        TeacherDetailAdminDialog(
            teacher = activeTeacher,
            onDismiss = { selectedTeacherForDetail = null },
            onToggleChecklist = { itemType, isCompleted ->
                viewModel.updateTeacherChecklistItem(activeTeacher.no, itemType, isCompleted)
                selectedTeacherForDetail = allSukajayaTeachers.find { it.no == activeTeacher.no }
            },
            onToggleVerify = {
                viewModel.toggleTeacherVerification(activeTeacher.no)
                selectedTeacherForDetail = allSukajayaTeachers.find { it.no == activeTeacher.no }
                Toast.makeText(
                    context,
                    if (!activeTeacher.verifiedByKkg) "Data guru ${activeTeacher.name} diverifikasi KKG!" else "Verifikasi dibatalkan.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCopySummary = {
                val summaryText = """
                    REKAP DATA GURU PAI KEC. SUKAJAYA
                    No: ${activeTeacher.no}
                    Nama: ${activeTeacher.name}
                    Akun SIAGA: ${activeTeacher.akunSiaga}
                    NUPTK: ${activeTeacher.nuptk}
                    Status Kepegawaian: ${activeTeacher.statusKepegawaian}
                    Sertifikasi: ${if (activeTeacher.isCertified) "SERTIFIKASI (YA)" else "BELUM SERTIFIKASI"}
                    Satminkal: ${activeTeacher.satminkal} (NPSN: ${activeTeacher.npsn})
                    Desa: ${activeTeacher.desa}
                    Kelengkapan Berkas: ${activeTeacher.progressPercent}% (${activeTeacher.status.label})
                    Status Verifikasi: ${if (activeTeacher.verifiedByKkg) "TERVERIFIKASI KKG" else "BELUM VERIFIKASI"}
                """.trimIndent()
                clipboardManager.setText(AnnotatedString(summaryText))
                Toast.makeText(context, "Data ${activeTeacher.name} disalin ke clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Modal Dialog Struktur Pengurus KKG PAI Sukajaya ---
    if (showKkgPengurusDialog) {
        KkgStructureDialog(
            summary = sukajayaSummary,
            onDismiss = { showKkgPengurusDialog = false }
        )
    }
}

// =========================================================================
// SUB-KOMPONEN KARTU & ITEM MONITORING KEC. SUKAJAYA
// =========================================================================

@Composable
fun SukajayaKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun KepegawaianLegendItem(
    label: String,
    count: String,
    percent: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = "$label ($percent)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = count,
                fontSize = 9.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AdminStatusCard(
    modifier: Modifier = Modifier,
    label: String,
    count: String,
    subtext: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) color else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else TextDark
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else TextDark,
                maxLines = 1
            )
            Text(
                text = subtext,
                fontSize = 8.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SukajayaSchoolCard(
    school: SukajayaSchoolProgress,
    onViewDetail: () -> Unit,
    onToggleVerify: () -> Unit
) {
    val statusColor = when (school.status) {
        SchoolAdminStatus.LENGKAP -> GreenPrimary
        SchoolAdminStatus.DALAM_PROSES -> GoldAccent
        SchoolAdminStatus.PERLU_PENDAMPINGAN -> Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewDetail() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: School Name, Village & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = school.schoolName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Desa ${school.desa}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenPrimary
                        )
                        Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "NPSN ${school.npsn}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = school.status.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Teachers at Satminkal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (school.teachers.isNotEmpty()) {
                    school.teachers.forEach { teacherItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${teacherItem.no}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = teacherItem.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "SIAGA: ${teacherItem.akunSiaga} • NUPTK: ${teacherItem.nuptk}",
                                        fontSize = 8.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = when {
                                        teacherItem.statusKepegawaian.startsWith("PNS") -> GoldAccent.copy(alpha = 0.15f)
                                        teacherItem.statusKepegawaian.startsWith("PPPK") -> GreenPrimary.copy(alpha = 0.15f)
                                        else -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = teacherItem.statusKepegawaian,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            teacherItem.statusKepegawaian.startsWith("PNS") -> GoldAccent
                                            teacherItem.statusKepegawaian.startsWith("PPPK") -> GreenPrimary
                                            else -> Color(0xFF0284C7)
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                if (teacherItem.isCertified) {
                                    Surface(
                                        color = GoldAccent,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Sergu",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = school.teacherName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = if (school.teacherNip != "-") "NIP. ${school.teacherNip}" else "Guru Non-ASN",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Surface(
                            color = GreenPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = school.employmentStatus,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kelengkapan Berkas Administrasi",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${school.progressPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(school.progressPercent / 100f)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini Checklist 7 Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniChecklistTag(label = "CP/ATP", isDone = school.cpAtpDone)
                MiniChecklistTag(label = "Modul", isDone = school.modulAjarDone)
                MiniChecklistTag(label = "Prota", isDone = school.protaPromesDone)
                MiniChecklistTag(label = "KKTP", isDone = school.kktpDone)
                MiniChecklistTag(label = "Jurnal", isDone = school.jurnalDone)
                MiniChecklistTag(label = "Asesmen", isDone = school.asesmenDone)
                MiniChecklistTag(label = "Ekstra", isDone = school.ekstraDone)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions & Verification Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (school.verifiedByKkg) Icons.Default.CheckCircle else Icons.Default.Pending,
                        contentDescription = null,
                        tint = if (school.verifiedByKkg) GreenPrimary else Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (school.verifiedByKkg) "Terverifikasi KKG" else "Menunggu Verifikasi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (school.verifiedByKkg) GreenPrimary else Color(0xFFB45309)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onToggleVerify,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (school.verifiedByKkg) "Batal Verifikasi" else "Verifikasi",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (school.verifiedByKkg) Color.Gray else GreenPrimary
                        )
                    }

                    FilledTonalButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GreenPrimary.copy(alpha = 0.1f),
                            contentColor = GreenPrimary
                        )
                    ) {
                        Text("Rincian", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MiniChecklistTag(label: String, isDone: Boolean) {
    Surface(
        color = if (isDone) GreenPrimary.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "$label ${if (isDone) "✓" else "–"}",
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDone) GreenPrimary else Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// =========================================================================
// KARTU GURU PAI SUKAJAYA (SUB-TAB 33 GURU PAI)
// =========================================================================

@Composable
fun SukajayaTeacherCard(
    teacher: SukajayaTeacherItem,
    onViewDetail: () -> Unit,
    onToggleVerify: () -> Unit
) {
    val statusColor = when (teacher.status) {
        SchoolAdminStatus.LENGKAP -> GreenPrimary
        SchoolAdminStatus.DALAM_PROSES -> GoldAccent
        SchoolAdminStatus.PERLU_PENDAMPINGAN -> Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_card_${teacher.no}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Number + Teacher Name + Satminkal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${teacher.no}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = teacher.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = teacher.satminkal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = teacher.status.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details: SIAGA, NUPTK, Status & Sertifikasi
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Akun SIAGA: ${teacher.akunSiaga}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                        Text(
                            text = "NUPTK: ${teacher.nuptk}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = when {
                                    teacher.statusKepegawaian.startsWith("PNS") -> GoldAccent.copy(alpha = 0.15f)
                                    teacher.statusKepegawaian.startsWith("PPPK") -> GreenPrimary.copy(alpha = 0.15f)
                                    else -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = teacher.statusKepegawaian,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        teacher.statusKepegawaian.startsWith("PNS") -> GoldAccent
                                        teacher.statusKepegawaian.startsWith("PPPK") -> GreenPrimary
                                        else -> Color(0xFF0284C7)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = if (teacher.isCertified) GoldAccent else Color(0xFF94A3B8).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (teacher.isCertified) "SERTIFIKASI" else "NON-SERTI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (teacher.isCertified) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Desa ${teacher.desa}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kelengkapan Berkas Administrasi",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${teacher.progressPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(teacher.progressPercent / 100f)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini Checklist 7 Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniChecklistTag(label = "CP/ATP", isDone = teacher.cpAtpDone)
                MiniChecklistTag(label = "Modul", isDone = teacher.modulAjarDone)
                MiniChecklistTag(label = "Prota", isDone = teacher.protaPromesDone)
                MiniChecklistTag(label = "KKTP", isDone = teacher.kktpDone)
                MiniChecklistTag(label = "Jurnal", isDone = teacher.jurnalDone)
                MiniChecklistTag(label = "Asesmen", isDone = teacher.asesmenDone)
                MiniChecklistTag(label = "Ekstra", isDone = teacher.ekstraDone)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions & Verification Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (teacher.verifiedByKkg) Icons.Default.CheckCircle else Icons.Default.Pending,
                        contentDescription = null,
                        tint = if (teacher.verifiedByKkg) GreenPrimary else Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (teacher.verifiedByKkg) "Terverifikasi KKG" else "Menunggu Verifikasi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (teacher.verifiedByKkg) GreenPrimary else Color(0xFFB45309)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onToggleVerify,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (teacher.verifiedByKkg) "Batal Verifikasi" else "Verifikasi",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (teacher.verifiedByKkg) Color.Gray else GreenPrimary
                        )
                    }

                    FilledTonalButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GreenPrimary.copy(alpha = 0.1f),
                            contentColor = GreenPrimary
                        )
                    ) {
                        Text("Rincian", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// DIALOG DETAIL PROGRES ADMINISTRASI GURU PAI
// =========================================================================

@Composable
fun TeacherDetailAdminDialog(
    teacher: SukajayaTeacherItem,
    onDismiss: () -> Unit,
    onToggleChecklist: (String, Boolean) -> Unit,
    onToggleVerify: () -> Unit,
    onCopySummary: () -> Unit
) {
    val statusColor = when (teacher.status) {
        SchoolAdminStatus.LENGKAP -> GreenPrimary
        SchoolAdminStatus.DALAM_PROSES -> GoldAccent
        SchoolAdminStatus.PERLU_PENDAMPINGAN -> Color(0xFFDC2626)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${teacher.no}. ${teacher.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Satminkal: ${teacher.satminkal} • Desa ${teacher.desa}",
                            fontSize = 11.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Progres Berkas Kurikulum Merdeka",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${teacher.progressPercent}% (${teacher.status.label})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Surface(
                            color = if (teacher.verifiedByKkg) GreenPrimary else GoldAccent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (teacher.verifiedByKkg) "TERVERIFIKASI" else "MENUNGGU",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Credentials Info Card (SIAGA, NUPTK, Status)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Akun SIAGA:", fontSize = 10.sp, color = Color.Gray)
                            Text(text = teacher.akunSiaga, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "NUPTK:", fontSize = 10.sp, color = Color.Gray)
                            Text(text = teacher.nuptk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Status Kepegawaian:", fontSize = 10.sp, color = Color.Gray)
                            Text(text = teacher.statusKepegawaian, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Sertifikasi PAI:", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = if (teacher.isCertified) "SERTIFIKASI (YA)" else "BELUM SERTIFIKASI (NON)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (teacher.isCertified) GoldAccent else Color.Gray
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "NPSN Satminkal:", fontSize = 10.sp, color = Color.Gray)
                            Text(text = teacher.npsn, fontSize = 11.sp, color = TextDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Kelengkapan 7 Berkas Administrasi:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Checklist Items
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminChecklistItem(
                        title = "1. CP & Alur Tujuan Pembelajaran (ATP)",
                        subtitle = "Berdasarkan SK BSKAP No. 020 Tahun 2026",
                        isChecked = teacher.cpAtpDone,
                        onCheckedChange = { onToggleChecklist("CP_ATP", it) }
                    )
                    AdminChecklistItem(
                        title = "2. Modul Ajar / RPP Kurikulum Merdeka",
                        subtitle = "Mencakup 5 Dimensi Cinta & diferensiasi",
                        isChecked = teacher.modulAjarDone,
                        onCheckedChange = { onToggleChecklist("MODUL_AJAR", it) }
                    )
                    AdminChecklistItem(
                        title = "3. Program Tahunan & Semester (Prota/Promes)",
                        subtitle = "Alokasi jam PAI & BP 3 JP per pekan",
                        isChecked = teacher.protaPromesDone,
                        onCheckedChange = { onToggleChecklist("PROTA_PROMES", it) }
                    )
                    AdminChecklistItem(
                        title = "4. Kriteria Ketercapaian TP (KKTP)",
                        subtitle = "Rubrik interval nilai capaian",
                        isChecked = teacher.kktpDone,
                        onCheckedChange = { onToggleChecklist("KKTP", it) }
                    )
                    AdminChecklistItem(
                        title = "5. Jurnal Mengajar & Presensi Harian",
                        subtitle = "Catatan materi dan kehadiran siswa",
                        isChecked = teacher.jurnalDone,
                        onCheckedChange = { onToggleChecklist("JURNAL", it) }
                    )
                    AdminChecklistItem(
                        title = "6. Buku Penilaian / Asesmen PAI & P5",
                        subtitle = "Formatif, sumatif, & portofolio",
                        isChecked = teacher.asesmenDone,
                        onCheckedChange = { onToggleChecklist("ASESMEN", it) }
                    )
                    AdminChecklistItem(
                        title = "7. Program Pembiasaan & Ekstrakurikuler",
                        subtitle = "Rohis, Salat Dhuha, & Tadarus Al-Qur'an",
                        isChecked = teacher.ekstraDone,
                        onCheckedChange = { onToggleChecklist("EKSTRA", it) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Catatan Verifikator KKG:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = teacher.notes,
                                fontSize = 11.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Terakhir diverifikasi: ${teacher.lastVerifiedDate}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopySummary,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Data", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onToggleVerify,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (teacher.verifiedByKkg) Color(0xFFB45309) else GreenPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (teacher.verifiedByKkg) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (teacher.verifiedByKkg) "Batal Verifikasi" else "Sahkan KKG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// DIALOG DETAIL PROGRES ADMINISTRASI SEKOLAH
// =========================================================================

@Composable
fun SchoolDetailAdminDialog(
    school: SukajayaSchoolProgress,
    onDismiss: () -> Unit,
    onToggleChecklist: (String, Boolean) -> Unit,
    onToggleVerify: () -> Unit,
    onCopySummary: () -> Unit
) {
    val statusColor = when (school.status) {
        SchoolAdminStatus.LENGKAP -> GreenPrimary
        SchoolAdminStatus.DALAM_PROSES -> GoldAccent
        SchoolAdminStatus.PERLU_PENDAMPINGAN -> Color(0xFFDC2626)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = school.schoolName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Desa ${school.desa}, Kec. Sukajaya • NPSN: ${school.npsn}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Progres Administrasi Kurmer CP 020",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${school.progressPercent}% (${school.status.label})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Surface(
                            color = if (school.verifiedByKkg) GreenPrimary else GoldAccent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (school.verifiedByKkg) "TERVERIFIKASI" else "MENUNGGU",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Teacher Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = school.teacherName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "${school.employmentStatus} • NIP. ${school.teacherNip} • ${school.classesHandled}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Kelengkapan 7 Berkas Administrasi Utama:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Checklist Items
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminChecklistItem(
                        title = "1. CP & Alur Tujuan Pembelajaran (ATP)",
                        subtitle = "Berdasarkan SK BSKAP No. 020 Tahun 2026",
                        isChecked = school.cpAtpDone,
                        onCheckedChange = { onToggleChecklist("CP_ATP", it) }
                    )
                    AdminChecklistItem(
                        title = "2. Modul Ajar / RPP Kurikulum Merdeka",
                        subtitle = "Mencakup 5 Dimensi Cinta & diferensiasi ajar",
                        isChecked = school.modulAjarDone,
                        onCheckedChange = { onToggleChecklist("MODUL_AJAR", it) }
                    )
                    AdminChecklistItem(
                        title = "3. Program Tahunan & Semester (Prota/Promes)",
                        subtitle = "Alokasi jam PAI & BP 3 JP per pekan",
                        isChecked = school.protaPromesDone,
                        onCheckedChange = { onToggleChecklist("PROTA_PROMES", it) }
                    )
                    AdminChecklistItem(
                        title = "4. Kriteria Ketercapaian TP (KKTP)",
                        subtitle = "Rubrik interval nilai & deskripsi capaian",
                        isChecked = school.kktpDone,
                        onCheckedChange = { onToggleChecklist("KKTP", it) }
                    )
                    AdminChecklistItem(
                        title = "5. Jurnal Mengajar & Presensi Harian",
                        subtitle = "Catatan materi dan kehadiran siswa",
                        isChecked = school.jurnalDone,
                        onCheckedChange = { onToggleChecklist("JURNAL", it) }
                    )
                    AdminChecklistItem(
                        title = "6. Buku Penilaian / Asesmen PAI & P5",
                        subtitle = "Formatif, sumatif, & portofolio siswa",
                        isChecked = school.asesmenDone,
                        onCheckedChange = { onToggleChecklist("ASESMEN", it) }
                    )
                    AdminChecklistItem(
                        title = "7. Program Pembiasaan & Ekstrakurikuler",
                        subtitle = "Rohis, Salat Dhuha, & Tadarus Al-Qur'an",
                        isChecked = school.ekstraDone,
                        onCheckedChange = { onToggleChecklist("EKSTRA", it) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Catatan Verifikator KKG:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = school.notes,
                                fontSize = 11.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Terakhir diverifikasi: ${school.lastVerifiedDate}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopySummary,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Laporan", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onToggleVerify,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (school.verifiedByKkg) Color(0xFFB45309) else GreenPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (school.verifiedByKkg) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (school.verifiedByKkg) "Batal Verifikasi" else "Sahkan KKG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminChecklistItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!isChecked) },
        color = if (isChecked) GreenPrimary.copy(alpha = 0.06f) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isChecked) GreenPrimary.copy(alpha = 0.3f) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = GreenPrimary,
                    uncheckedColor = Color.Gray
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked) GreenPrimary else TextDark
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
            if (isChecked) {
                Surface(
                    color = GreenPrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Lengkap",
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// DIALOG STRUKTUR PENGURUS KKG PAI KEC. SUKAJAYA
// =========================================================================

@Composable
fun KkgStructureDialog(
    summary: SukajayaPaiSummary,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "KKG PAI KECAMATAN SUKAJAYA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Kelompok Kerja Guru Pendidikan Agama Islam SD\nKabupaten Bogor, Jawa Barat",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KkgPersonRow(jabatan = "Ketua KKG", nama = summary.kkgChairperson, unit = "SDN Kiarapandak 01")
                    KkgPersonRow(jabatan = "Sekretaris", nama = summary.kkgSecretary, unit = "SDN Kiarapandak 02")
                    KkgPersonRow(jabatan = "Bendahara", nama = summary.kkgTreasurer, unit = "SDN Sukajaya 01")
                    KkgPersonRow(jabatan = "Pengawas Pembina", nama = "Drs. H. Mulyana, M.Pd.I", unit = "Kemenag Kab. Bogor")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Tutup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KkgPersonRow(jabatan: String, nama: String, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = jabatan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Text(
                text = nama,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = unit,
                fontSize = 9.sp,
                color = Color.Gray
            )
        }
    }
}

// =========================================================================
// HELPER COMPOSABLES ASLI DARI SISTEM DASHBOARD
// =========================================================================

@Composable
fun AlertItem(
    title: String,
    desc: String,
    actionText: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text(text = desc, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(contentColor = color)
            ) {
                Text(text = actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
            Column {
                Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = title, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ScheduleItem(time: String, className: String, subject: String, isCompleted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCompleted) Color.LightGray.copy(alpha = 0.15f) else GreenPrimary.copy(alpha = 0.05f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isCompleted) Color.LightGray.copy(alpha = 0.3f) else GreenPrimary.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = time, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isCompleted) Color.Gray else GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isCompleted) Color.Gray.copy(alpha = 0.2f) else GoldAccent.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = className,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color.DarkGray else GreenPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subject,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) Color.Gray else TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF4CAF50) else GoldAccent
        )
    }
}

@Composable
fun CalendarAgendaItem(date: String, title: String, type: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GreenPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = GreenPrimary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = type,
                fontSize = 11.sp,
                color = if (type == "Keagamaan") GoldAccent else Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProgressItem(title: String, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(text = "${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                .clip(CircleShape)
                .background(color)
            )
        }
    }
}
