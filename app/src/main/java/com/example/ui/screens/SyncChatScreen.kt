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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.models.*
import com.example.ui.AppViewModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenSecondary
import com.example.util.SecurityUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncChatScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncRepo = viewModel.syncRepository

    // State Collection
    val currentUser by syncRepo.currentUser.collectAsStateWithLifecycle()
    val pairedDevices by syncRepo.pairedDevices.collectAsStateWithLifecycle()
    val syncHistory by syncRepo.syncHistory.collectAsStateWithLifecycle()
    val messages by syncRepo.messages.collectAsStateWithLifecycle()
    val currentMode by syncRepo.currentConnectionMode.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: User & Sync, 1: Chat PAI, 2: Koneksi Perangkat

    // User Edit / Registration state
    var showRegisterDialog by remember { mutableStateOf(false) }
    var regNip by remember { mutableStateOf("") }
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("Guru PAI") }

    // Sync progress dialog
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncLogText by remember { mutableStateOf("") }

    // Chat Message inputs
    var textMessage by remember { mutableStateOf("") }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    // Setup Local DB database access for sync callback
    val appDb = AppDatabase.getDatabase(context, coroutineScope)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Selection Header
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = GreenPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = GoldAccent
                )
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("User & Sinkronisasi", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Chat KKG PAI", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Jaringan Lokal", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SettingsEthernet, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Active Connection Mode bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (currentMode) {
                    ConnectionType.INTERNET_CLOUD -> Color(0xFFE8F5E9)
                    ConnectionType.WIFI_LOCAL -> Color(0xFFE3F2FD)
                    else -> Color(0xFFFFF3E0)
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (currentMode) {
                        ConnectionType.INTERNET_CLOUD -> Icons.Default.CloudDone
                        ConnectionType.WIFI_LOCAL -> Icons.Default.Wifi
                        ConnectionType.WIFI_DIRECT -> Icons.Default.PortableWifiOff
                        ConnectionType.BLUETOOTH -> Icons.Default.BluetoothConnected
                    },
                    contentDescription = null,
                    tint = when (currentMode) {
                        ConnectionType.INTERNET_CLOUD -> GreenPrimary
                        ConnectionType.WIFI_LOCAL -> Color(0xFF1E88E5)
                        else -> Color(0xFFE65100)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Metode Koneksi Aktif: ${currentMode.name.replace("_", " ")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = if (isOnline) "Koneksi cloud Supabase PostgreSQL & Realtime aktif ($pendingSyncCount antrean)."
                               else "Internet terputus! Perubahan tersimpan di antrean offline ($pendingSyncCount) untuk dicoba ulang.",
                        fontSize = 9.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                0 -> { // --- TAB 0: USER PROFILE & SYNC LOGS ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Profile Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(GreenPrimary, GreenSecondary)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser?.name?.firstOrNull()?.toString() ?: "U",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentUser?.name ?: "Belum Terdaftar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "NIP. ${currentUser?.nip ?: "-"}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (currentUser?.isOnline == true) Color.Green else Color.Gray)
                                            )
                                            Text(
                                                text = if (currentUser?.isOnline == true) "Status: Online" else "Status: Offline (Lokal Only)",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            regNip = currentUser?.nip ?: ""
                                            regName = currentUser?.name ?: ""
                                            regEmail = currentUser?.email ?: ""
                                            regRole = currentUser?.userRole ?: "Guru PAI"
                                            showRegisterDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, GreenPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ubah Profil", fontSize = 11.sp, color = GreenPrimary)
                                    }

                                    Button(
                                        onClick = {
                                            syncLogText = "Memulai sinkronisasi Supabase PostgreSQL & Realtime..."
                                            showSyncDialog = true
                                            viewModel.retryPendingSync { msg ->
                                                syncLogText += "\n-> $msg"
                                            }
                                            syncRepo.performManualSync(
                                                db = appDb,
                                                onProgress = { log ->
                                                    syncLogText += "\n-> $log"
                                                },
                                                onComplete = {
                                                    syncLogText += "\n\nSelesai! Seluruh data & antrean offline berhasil disinkronkan ke Supabase."
                                                }
                                            )
                                        },
                                        modifier = Modifier.weight(1f).testTag("sync_cloud_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (pendingSyncCount > 0) "Sync ($pendingSyncCount)" else "Sync Cloud",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Encryption & Security Highlight Info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Https, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Protokol Keamanan & Sinkronisasi Multi-Perangkat", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(
                                    text = "Seluruh data disinkronkan ke Supabase PostgreSQL via HTTPS & WebSocket Realtime dengan Row Level Security (RLS) untuk peran Super Admin, Admin, Guru, dan Siswa. Pesan KKG PAI juga terenkripsi AES-256.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Sync Logs list
                        Text(
                            text = "Histori Sinkronisasi & Log Real-Time",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GreenSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (syncHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada log sinkronisasi.", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                syncHistory.forEach { log ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (log.syncStatus == "SUCCESS") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (log.syncStatus == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                                    contentDescription = null,
                                                    tint = if (log.syncStatus == "SUCCESS") Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${log.actionType}: ${log.entityType} (${log.dataId})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Terkoneksi via: ${log.peerDeviceName ?: "Lokal Sync"}",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(log.timestamp),
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                1 -> { // --- TAB 1: REAL-TIME CHAT / COMMUNICATION PANEL ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Message List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFF7F9FA))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                val isMe = msg.senderNip == currentUser?.nip
                                val decryptedText = SecurityUtil.decrypt(msg.messageText)

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        // Sender name if not me
                                        if (!isMe) {
                                            Text(
                                                text = msg.senderName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenPrimary,
                                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                            )
                                        }

                                        // Message Bubble
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 12.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMe) GreenPrimary else Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                // Handle Rich Attachments
                                                if (msg.fileType != FileType.TEXT) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                if (isMe) Color.White.copy(alpha = 0.15f)
                                                                else Color.LightGray.copy(alpha = 0.2f)
                                                            )
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = when (msg.fileType) {
                                                                FileType.PHOTO -> Icons.Default.Photo
                                                                FileType.VIDEO -> Icons.Default.Videocam
                                                                FileType.DOCUMENT -> Icons.Default.InsertDriveFile
                                                                FileType.LOCATION -> Icons.Default.LocationOn
                                                                FileType.VOICE -> Icons.Default.Mic
                                                                else -> Icons.Default.Attachment
                                                            },
                                                            contentDescription = null,
                                                            tint = if (isMe) Color.White else GreenPrimary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = msg.fileName ?: "Lampiran Media",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMe) Color.White else Color.Black
                                                            )
                                                            Text(
                                                                text = msg.fileSize ?: "Media File",
                                                                fontSize = 9.sp,
                                                                color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }

                                                // Message Text
                                                if (decryptedText.isNotBlank()) {
                                                    Text(
                                                        text = decryptedText,
                                                        fontSize = 13.sp,
                                                        color = if (isMe) Color.White else Color.Black
                                                    )
                                                }
                                            }
                                        }

                                        // Delivery and Connection details
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                        ) {
                                            Text(
                                                text = "${msg.connectionMethod.name.replace("_", " ")} • ${
                                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(msg.timestamp)
                                                }",
                                                fontSize = 8.sp,
                                                color = Color.Gray
                                            )
                                            if (isMe) {
                                                Icon(
                                                    imageVector = when (msg.status) {
                                                        MessageStatus.SENDING -> Icons.Default.HourglassEmpty
                                                        MessageStatus.SENT -> Icons.Default.Check
                                                        MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                                        MessageStatus.READ -> Icons.Default.DoneAll
                                                    },
                                                    contentDescription = null,
                                                    tint = if (msg.status == MessageStatus.READ) Color(0xFF00E676) else Color.Gray,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Simulated Peer Typing Indicator
                        pairedDevices.find { it.isTyping }?.let { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF7F9FA))
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${peer.name} sedang mengetik...",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Send message bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showAttachmentDialog = true },
                                    modifier = Modifier.testTag("attachment_button")
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "Lampirkan file", tint = GreenPrimary)
                                }

                                OutlinedTextField(
                                    value = textMessage,
                                    onValueChange = { textMessage = it },
                                    placeholder = { Text("Ketik pesan PAI...", fontSize = 13.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 80.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenPrimary,
                                        unfocusedBorderColor = Color.LightGray
                                    ),
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                FloatingActionButton(
                                    onClick = {
                                        if (textMessage.isNotBlank()) {
                                            syncRepo.sendMessage(textMessage)
                                            textMessage = ""
                                        }
                                    },
                                    containerColor = GreenPrimary,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("send_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                2 -> { // --- TAB 2: NEIGHBORHOOD DEVICES (WiFi Direct, Bluetooth, WiFi Local) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pencarian Perangkat Terdekat (Offline Mode)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GreenSecondary
                        )
                        Text(
                            text = "Sistem dapat mendeteksi perangkat Android di sekitar yang menggunakan aplikasi SIM Administrasi Guru PAI melalui jaringan lokal (WiFi / Bluetooth) secara otomatis tanpa kuota internet.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Mulai memindai perangkat lokal...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pindai Perangkat Baru", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Devices list
                        pairedDevices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (device.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (device.connectionType) {
                                                ConnectionType.WIFI_LOCAL -> Icons.Default.Wifi
                                                ConnectionType.WIFI_DIRECT -> Icons.Default.PortableWifiOff
                                                ConnectionType.BLUETOOTH -> Icons.Default.Bluetooth
                                                else -> Icons.Default.Cloud
                                            },
                                            contentDescription = null,
                                            tint = if (device.isOnline) GreenPrimary else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "NIP. ${device.nip}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Jalur: ${device.connectionType.name} • Kuat Sinyal: ${device.signalStrength}%",
                                            fontSize = 9.sp,
                                            color = Color.DarkGray
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Terkoneksi dengan ${device.name}!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (device.isOnline) GreenPrimary else Color.Gray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text(if (device.isOnline) "Hubungkan" else "Offline", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Register & Edit Profile Dialog
    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Registrasi / Ubah Profil", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regNip,
                        onValueChange = { regNip = it },
                        label = { Text("NIP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = syncRepo.registerUser(regNip, regName, regEmail, regRole)
                        if (success) {
                            showRegisterDialog = false
                            Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal! Periksa kesesuaian NIP (Hanya angka, min 8)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Sync Cloud progress Dialog
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = GreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proses Sinkronisasi", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Melakukan sinkronisasi database lokal Room dengan Supabase PostgreSQL & Realtime (status: $syncState).", fontSize = 12.sp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = syncLogText,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSyncDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        )
    }

    // Rich Media Attachment Dialog
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = { Text("Kirim Lampiran Media", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Photo
                    ListItem(
                        headlineContent = { Text("Foto / Kamera", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.Photo, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.clickable {
                            syncRepo.sendMessage(
                                text = "Mengirimkan lampiran foto...",
                                fileUri = "content://media/photos/1",
                                fileType = FileType.PHOTO,
                                fileName = "Dokumentasi_KKG.jpg",
                                fileSize = "1.2 MB"
                            )
                            showAttachmentDialog = false
                        }
                    )
                    // Video
                    ListItem(
                        headlineContent = { Text("Video", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.Videocam, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.clickable {
                            syncRepo.sendMessage(
                                text = "Mengirimkan lampiran video...",
                                fileUri = "content://media/video/1",
                                fileType = FileType.VIDEO,
                                fileName = "Video_P5_Merdeka.mp4",
                                fileSize = "24.5 MB"
                            )
                            showAttachmentDialog = false
                        }
                    )
                    // Document
                    ListItem(
                        headlineContent = { Text("Dokumen (Modul Ajar/Excel)", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.clickable {
                            syncRepo.sendMessage(
                                text = "Mengirimkan lampiran Dokumen...",
                                fileUri = "content://media/docs/1",
                                fileType = FileType.DOCUMENT,
                                fileName = "Modul_Ajar_PAI_IV.pdf",
                                fileSize = "450 KB"
                            )
                            showAttachmentDialog = false
                        }
                    )
                    // Location
                    ListItem(
                        headlineContent = { Text("Lokasi Saya", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.clickable {
                            syncRepo.sendMessage(
                                text = "Lokasi: SDN kiarapandak 01, Bogor",
                                fileUri = null,
                                fileType = FileType.LOCATION,
                                fileName = "SDN kiarapandak 01, Bogor",
                                fileSize = "-6.5971, 106.7942",
                                latitude = -6.5971,
                                longitude = 106.7942
                            )
                            showAttachmentDialog = false
                        }
                    )
                    // Voice note
                    ListItem(
                        headlineContent = { Text("Pesan Suara (Voice Note)", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.Mic, contentDescription = null, tint = GreenPrimary) },
                        modifier = Modifier.clickable {
                            syncRepo.sendMessage(
                                text = "Mengirimkan pesan suara...",
                                fileUri = "content://media/audio/1",
                                fileType = FileType.VOICE,
                                fileName = "VoiceNote_01.aac",
                                fileSize = "0:15",
                                duration = 15
                            )
                            showAttachmentDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
