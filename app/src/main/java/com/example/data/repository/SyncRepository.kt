package com.example.data.repository

import android.app.Application
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.models.*
import com.example.data.remote.RemoteBackendGateway
import com.example.data.remote.SupabaseTables
import com.example.util.SecurityUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class SyncRepository(
    private val application: Application,
    private val db: AppDatabase,
    private val syncEngine: CloudSyncEngine,
    private val gateway: RemoteBackendGateway
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PeerDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PeerDevice>> = _pairedDevices.asStateFlow()

    val syncHistory: StateFlow<List<SyncHistoryLog>> = db.syncHistoryLogDao()
        .getRecentSyncHistory()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val messages: StateFlow<List<SyncMessage>> = db.syncMessageDao()
        .getAllMessages()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    private val _currentConnectionMode = MutableStateFlow(ConnectionType.INTERNET_CLOUD)
    val currentConnectionMode: StateFlow<ConnectionType> = _currentConnectionMode.asStateFlow()

    init {
        repositoryScope.launch {
            syncEngine.isNetworkConnected.collect { connected ->
                _currentConnectionMode.value = if (connected) {
                    ConnectionType.INTERNET_CLOUD
                } else {
                    ConnectionType.WIFI_LOCAL
                }
            }
        }
        repositoryScope.launch {
            db.managedUserDao().getAllUsers().collect { users ->
                _pairedDevices.value = users.map { u ->
                    PeerDevice(
                        deviceId = u.id,
                        name = "${u.fullName} (${AppRole.fromDbValue(u.role).displayName})",
                        nip = u.nip.ifBlank { u.email },
                        isOnline = u.isActive,
                        connectionType = _currentConnectionMode.value,
                        signalStrength = if (u.isActive) 95 else 0
                    )
                }
            }
        }
    }

    fun bindAuthenticatedProfile(profile: ManagedUserProfile?) {
        _currentUser.value = profile?.let {
            UserProfile(
                nip = it.nip.ifBlank { it.email },
                name = it.fullName,
                email = it.email,
                isOnline = syncEngine.isNetworkConnected.value,
                userRole = it.appRole.displayName
            )
        }
    }

    fun registerUser(nip: String, name: String, email: String, role: String): Boolean {
        if (name.isBlank() || email.isBlank()) return false
        _currentUser.value = UserProfile(
            nip = nip.ifBlank { email },
            name = name,
            email = email,
            isOnline = syncEngine.isNetworkConnected.value,
            userRole = role
        )
        return true
    }

    fun addSyncLog(entity: String, action: String, dataId: String, status: String, peer: String?) {
        repositoryScope.launch {
            db.syncHistoryLogDao().insert(
                SyncHistoryLog(
                    timestamp = System.currentTimeMillis(),
                    entityType = entity,
                    actionType = action,
                    dataId = dataId,
                    syncStatus = status,
                    peerDeviceName = peer
                )
            )
        }
    }

    fun updateOnlineStatus(online: Boolean) {
        _currentUser.value = _currentUser.value?.copy(isOnline = online)
        _currentConnectionMode.value = if (online) ConnectionType.INTERNET_CLOUD else ConnectionType.WIFI_LOCAL
        if (online) {
            repositoryScope.launch {
                syncEngine.syncNow(forceRetry = true)
            }
        }
    }

    fun sendMessage(
        text: String,
        fileUri: String? = null,
        fileType: FileType = FileType.TEXT,
        fileName: String? = null,
        fileSize: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        duration: Int? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank() && fileUri == null) return
        val user = _currentUser.value ?: return
        val encryptedText = SecurityUtil.encrypt(cleanText)

        val newMessage = SyncMessage(
            id = "msg-${UUID.randomUUID()}",
            senderNip = user.nip,
            senderName = user.name,
            receiverNip = "ALL",
            messageText = encryptedText,
            fileUri = fileUri,
            fileType = fileType,
            fileName = fileName,
            fileSize = fileSize,
            locationLatitude = latitude,
            locationLongitude = longitude,
            durationSeconds = duration,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            connectionMethod = _currentConnectionMode.value
        )

        repositoryScope.launch {
            db.syncMessageDao().upsert(newMessage)
            val payload = syncEngine.encodeSyncMessage(newMessage, gateway.currentUserId)
            syncEngine.enqueueUpsert(SupabaseTables.SYNC_MESSAGES, newMessage.id, payload)
        }
    }

    fun performManualSync(
        db: AppDatabase,
        onProgress: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        repositoryScope.launch {
            try {
                onProgress("Menghubungkan ke Supabase PostgreSQL (${com.example.data.remote.SupabaseConfig.supabaseUrl})...")
                val pendingBefore = db.pendingSyncDao().getAllPending().size
                onProgress("Mengirim $pendingBefore antrean perubahan lokal ke cloud...")
                val result = syncEngine.syncNow(forceRetry = true)
                if (result.isSuccess) {
                    onProgress("Mengunduh pembaruan terbaru dari semua perangkat via Supabase Realtime...")
                    onProgress("Sinkronisasi multi-perangkat selesai 100%.")
                } else {
                    onProgress("Sebagian data tersimpan di antrean offline: ${result.exceptionOrNull()?.localizedMessage}")
                }
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("SyncRepository", "Sync error", e)
                onProgress("Terjadi kendala koneksi: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
