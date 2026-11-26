package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.BrightThemeMode
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Collect states reactively
    val teacher by viewModel.teacherProfile.collectAsStateWithLifecycle()
    val school by viewModel.schoolData.collectAsStateWithLifecycle()
    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val journals by viewModel.allJournals.collectAsStateWithLifecycle()
    val assessments by viewModel.allAssessments.collectAsStateWithLifecycle()
    val questions by viewModel.allQuestions.collectAsStateWithLifecycle()
    val activities by viewModel.allActivities.collectAsStateWithLifecycle()
    val extracurriculars by viewModel.allExtracurriculars.collectAsStateWithLifecycle()
    val logs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val currentAppRole by viewModel.currentAppRole.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val currentBrightTheme by viewModel.currentBrightTheme.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Navigation states
    var currentBottomTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Kelas, 2: Perangkat, 3: Asisten AI
    var selectedDrawerMenu by remember { mutableStateOf<String?>(null) } // Null means showing current bottom tab

    BackHandler(enabled = drawerState.isOpen || selectedDrawerMenu != null || currentBottomTab != 0) {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            selectedDrawerMenu != null -> selectedDrawerMenu = null
            currentBottomTab != 0 -> currentBottomTab = 0
        }
    }

    // Navigation Drawer items (Admin & Guru management integrated cleanly)
    val drawerItems = buildList {
        if (currentAppRole.canEditAcademicData) {
            add(
                DrawerMenuItem(
                    title = if (currentAppRole.isAdminOrAbove) "Dashboard Admin & Akses" else "Manajemen Data & Riwayat",
                    icon = Icons.Default.AdminPanelSettings,
                    id = "admin_dashboard"
                )
            )
        }
        add(DrawerMenuItem("Asisten AI Guru PAI", Icons.Default.AutoAwesome, "asisten_ai"))
        add(DrawerMenuItem("Studio Media AI (Veo & Gambar)", Icons.Default.VideoCameraBack, "studio_media"))
        add(DrawerMenuItem("Profil Guru", Icons.Default.Person, "profil"))
        add(DrawerMenuItem("Data Sekolah", Icons.Default.School, "sekolah"))
        add(DrawerMenuItem("Data Kelas & Siswa", Icons.Default.People, "kelas_siswa"))
        add(DrawerMenuItem("Presensi Siswa PAI", Icons.Default.FactCheck, "absensi"))
        add(DrawerMenuItem("Hafalan Surat Pendek", Icons.Default.AutoStories, "hafalan"))
        add(DrawerMenuItem("Perangkat Merdeka", Icons.Default.MenuBook, "perangkat"))
        add(DrawerMenuItem("Administrasi Mengajar", Icons.Default.Assignment, "administrasi"))
        add(DrawerMenuItem("Penilaian Siswa", Icons.Default.Grade, "penilaian"))
        add(DrawerMenuItem("Kokurikuler PAI", Icons.Default.Nature, "kokurikuler"))
        add(DrawerMenuItem("Rohis & Ekstrakurikuler", Icons.Default.Mosque, "rohis"))
        add(DrawerMenuItem("Projek P5 & PPRA", Icons.Default.Star, "p5"))
        add(DrawerMenuItem("Administrasi Keagamaan", Icons.Default.AutoAwesome, "keagamaan"))
        add(DrawerMenuItem("Bank Soal PAI", Icons.Default.Quiz, "bank_soal"))
        add(DrawerMenuItem("Sinkronisasi & Chat", Icons.Default.CloudSync, "sync_chat"))
        add(DrawerMenuItem("Arsip Digital & Cloud", Icons.Default.Folder, "arsip"))
        add(DrawerMenuItem("Laporan Administrasi", Icons.Default.Analytics, "laporan"))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header Drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GreenPrimary, GreenSecondary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                                .align(Alignment.Start),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = teacher.fullName.firstOrNull()?.toString() ?: "G",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentUserProfile?.fullName?.ifBlank { teacher.fullName } ?: teacher.fullName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUserProfile?.email?.ifBlank { "NIP. ${teacher.nip}" } ?: "NIP. ${teacher.nip}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUserProfile?.schoolName?.ifBlank { school.schoolName } ?: school.schoolName,
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Authenticated RBAC Badge & Logout Action
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hak Akses Akun (RLS):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = currentAppRole.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GreenPrimary
                            )
                        }
                        OutlinedButton(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.testTag("logout_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Keluar",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keluar", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pendingSyncCount > 0) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.retryPendingSync { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SyncProblem, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "$pendingSyncCount perubahan menunggu sync • Ketuk ulangi",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Drawer Menu List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(drawerItems) { item ->
                        val isSelected = selectedDrawerMenu == item.id
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null, tint = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface) },
                            label = { Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            selected = isSelected,
                            onClick = {
                                when (item.id) {
                                    "kelas_siswa" -> {
                                        selectedDrawerMenu = null
                                        currentBottomTab = 1
                                    }
                                    "perangkat" -> {
                                        selectedDrawerMenu = null
                                        currentBottomTab = 2
                                    }
                                    "asisten_ai" -> {
                                        selectedDrawerMenu = null
                                        currentBottomTab = 3
                                    }
                                    else -> {
                                        selectedDrawerMenu = item.id
                                    }
                                }
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.12f),
                                selectedTextColor = GreenPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                HorizontalDivider()
                // Pilihan Warna Background Cerah
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable {
                            showThemeDialog = true
                            coroutineScope.launch { drawerState.close() }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(currentBrightTheme.bgHex),
                    border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Warna: ${currentBrightTheme.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Ketuk untuk ganti warna cerah",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ADM PAI Kec. Sukajaya v.01",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                MediumTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (selectedDrawerMenu == null) {
                                // Custom green rotating card logo with yellow dot inside from High Density theme
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GreenPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Simulated rotated square
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .border(1.5.dp, Color.White, RoundedCornerShape(2.dp))
                                            .drawBehind {
                                                drawCircle(
                                                    color = GoldLight,
                                                    radius = size.minDimension * 0.2f
                                                )
                                            }
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = selectedDrawerMenu?.let { id -> drawerItems.find { it.id == id }?.title } ?: "Administrasi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = GreenSecondary
                                )
                                Text(
                                    text = if (selectedDrawerMenu != null) "${school.schoolName} • TA ${school.academicYear}" else "GURU PAI • KURIKULUM MERDEKA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (selectedDrawerMenu != null) {
                            IconButton(onClick = { selectedDrawerMenu = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                        } else {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu Sidebar")
                            }
                        }
                    },
                    actions = {
                        // Online / Offline & Pending Queue Badge
                        AssistChip(
                            onClick = {
                                viewModel.retryPendingSync { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = {
                                Text(
                                    when {
                                        !isOnline -> if (pendingSyncCount > 0) "Offline ($pendingSyncCount)" else "Offline"
                                        pendingSyncCount > 0 -> "Antrean ($pendingSyncCount)"
                                        else -> "Online"
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFE57373))
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        )

                        // Pilihan Warna Latar Cerah
                        IconButton(onClick = { showThemeDialog = true }) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Pilihan Warna Latar Cerah",
                                tint = GreenPrimary
                            )
                        }

                        // Cloud sync indicator
                        IconButton(
                            onClick = {
                                viewModel.retryPendingSync { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("topbar_sync_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sinkronkan ke Supabase", tint = GreenPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            bottomBar = {
                // Bottom NavigationBar on compact/medium phone widths; wide screens use NavigationRail
                BoxWithConstraints {
                    if (selectedDrawerMenu == null && maxWidth < 840.dp) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        ) {
                            NavigationBarItem(
                                selected = currentBottomTab == 0,
                                onClick = { currentBottomTab = 0 },
                                icon = { Icon(if (currentBottomTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard, contentDescription = null) },
                                label = { Text("Dashboard", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GreenPrimary,
                                    selectedTextColor = GreenPrimary,
                                    indicatorColor = GoldAccent.copy(alpha = 0.25f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentBottomTab == 1,
                                onClick = { currentBottomTab = 1 },
                                icon = { Icon(if (currentBottomTab == 1) Icons.Default.People else Icons.Outlined.People, contentDescription = null) },
                                label = { Text("Kelas & Siswa", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GreenPrimary,
                                    selectedTextColor = GreenPrimary,
                                    indicatorColor = GoldAccent.copy(alpha = 0.25f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentBottomTab == 2,
                                onClick = { currentBottomTab = 2 },
                                icon = { Icon(if (currentBottomTab == 2) Icons.Default.Assignment else Icons.Outlined.Assignment, contentDescription = null) },
                                label = { Text("Perangkat", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GreenPrimary,
                                    selectedTextColor = GreenPrimary,
                                    indicatorColor = GoldAccent.copy(alpha = 0.25f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentBottomTab == 3,
                                onClick = { currentBottomTab = 3 },
                                icon = { Icon(if (currentBottomTab == 3) Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome, contentDescription = null) },
                                label = { Text("Asisten AI", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GreenPrimary,
                                    selectedTextColor = GreenPrimary,
                                    indicatorColor = GoldAccent.copy(alpha = 0.25f)
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val useNavigationRail = maxWidth >= 840.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        ) {
                            NavigationRailItem(
                                selected = selectedDrawerMenu == null && currentBottomTab == 0,
                                onClick = {
                                    selectedDrawerMenu = null
                                    currentBottomTab = 0
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Beranda", fontSize = 11.sp) }
                            )
                            NavigationRailItem(
                                selected = selectedDrawerMenu == null && currentBottomTab == 1,
                                onClick = {
                                    selectedDrawerMenu = null
                                    currentBottomTab = 1
                                },
                                icon = { Icon(Icons.Default.People, contentDescription = "Kelas & Siswa") },
                                label = { Text("Siswa", fontSize = 11.sp) }
                            )
                            NavigationRailItem(
                                selected = selectedDrawerMenu == null && currentBottomTab == 2,
                                onClick = {
                                    selectedDrawerMenu = null
                                    currentBottomTab = 2
                                },
                                icon = { Icon(Icons.Default.Assignment, contentDescription = "Perangkat") },
                                label = { Text("Perangkat", fontSize = 11.sp) }
                            )
                            NavigationRailItem(
                                selected = selectedDrawerMenu == null && currentBottomTab == 3,
                                onClick = {
                                    selectedDrawerMenu = null
                                    currentBottomTab = 3
                                },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Asisten AI") },
                                label = { Text("Asisten AI", fontSize = 11.sp) }
                            )
                            if (currentAppRole.canEditAcademicData) {
                                NavigationRailItem(
                                    selected = selectedDrawerMenu == "admin_dashboard",
                                    onClick = { selectedDrawerMenu = "admin_dashboard" },
                                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Dashboard Admin") },
                                    label = { Text("Admin", fontSize = 11.sp) }
                                )
                            }
                            NavigationRailItem(
                                selected = selectedDrawerMenu == "arsip",
                                onClick = { selectedDrawerMenu = "arsip" },
                                icon = { Icon(Icons.Default.Folder, contentDescription = "Arsip Cloud") },
                                label = { Text("Arsip", fontSize = 11.sp) }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // If a sidebar drawer item is selected, we swap the content to that detailed screen.
                        // Otherwise we show the bottom navigation tab.
                        if (selectedDrawerMenu != null) {
                            when (selectedDrawerMenu) {
                                "admin_dashboard" -> AdminDashboardScreen(viewModel)
                                "profil" -> ProfilGuruScreen(viewModel, teacher)
                                "sekolah" -> DataSekolahScreen(viewModel, school)
                                "kelas_siswa" -> KelasSiswaScreen(viewModel, classes, students)
                                "absensi" -> AbsensiScreen(viewModel, onNavigateBack = { selectedDrawerMenu = null })
                                "hafalan" -> HafalanSuratScreen(viewModel, onNavigateBack = { selectedDrawerMenu = null })
                                "studio_media" -> MediaStudioScreen(viewModel, onNavigateBack = { selectedDrawerMenu = null })
                                "perangkat" -> PerangkatMerdekaScreen(viewModel, documents)
                                "administrasi" -> AdministrasiPembelajaranScreen(viewModel, journals, classes, students)
                                "penilaian" -> PenilaianSiswaScreen(viewModel, assessments, students, classes)
                                "kokurikuler" -> KokurikulerScreen(viewModel)
                                "rohis" -> RohisScreen(viewModel, extracurriculars)
                                "p5" -> ProjekP5Screen(viewModel)
                                "keagamaan" -> AdministrasiKeagamaanScreen(viewModel, activities)
                                "bank_soal" -> BankSoalScreen(viewModel, questions)
                                "sync_chat" -> SyncChatScreen(viewModel)
                                "arsip" -> ArsipDigitalScreen(viewModel)
                                "laporan" -> LaporanAdministrasiScreen(viewModel, journals, assessments, activities, extracurriculars, classes)
                            }
                        } else {
                            when (currentBottomTab) {
                                0 -> DashboardScreen(
                                    viewModel = viewModel,
                                    teacher = teacher,
                                    school = school,
                                    classes = classes,
                                    students = students,
                                    documents = documents,
                                    journals = journals,
                                    assessments = assessments,
                                    activities = activities,
                                    onNavigateToMenu = { menuId ->
                                        if (menuId == "asisten_ai") {
                                            currentBottomTab = 3
                                            selectedDrawerMenu = null
                                        } else {
                                            selectedDrawerMenu = menuId
                                        }
                                    }
                                )
                                1 -> KelasSiswaScreen(viewModel, classes, students)
                                2 -> PerangkatMerdekaScreen(viewModel, documents)
                                3 -> AsistenAIScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Keluar dari Akun?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Sesi Anda akan diakhiri. Pastikan tidak ada antrean sinkronisasi yang tertunda sebelum berpindah akun."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = GreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Warna Latar Cerah", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih latar belakang bernuansa cerah dan segar untuk kenyamanan administrasi:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (mode in BrightThemeMode.entries) {
                        val isSelected = currentBrightTheme == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectBrightTheme(mode)
                                    showThemeDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(mode.bgHex),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GreenPrimary else Color(0xFFCBD5E1)
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(mode.bgHex))
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) GreenPrimary else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = GreenPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Data class representation of Drawer menu items
data class DrawerMenuItem(val title: String, val icon: ImageVector, val id: String)
