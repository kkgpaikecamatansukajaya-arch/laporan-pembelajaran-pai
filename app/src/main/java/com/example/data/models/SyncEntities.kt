package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representasi profil pengguna untuk sinkronisasi dan komunikasi antar perangkat.
 */
@Entity(tableName = "sync_user_profiles")
data class UserProfile(
    @PrimaryKey val nip: String,
    val name: String,
    val email: String,
    val isOnline: Boolean = true,
    val lastActive: Long = System.currentTimeMillis(),
    val userRole: String = "Guru PAI",
    val encryptionKey: String = "KKG-PAI-SUKAJAYA-SECURE-KEY"
)

/**
 * Representasi perangkat rekan (peer) yang ditemukan dalam jaringan.
 */
data class PeerDevice(
    val deviceId: String,
    val name: String,
    val nip: String,
    val isOnline: Boolean,
    val connectionType: ConnectionType,
    val signalStrength: Int, // 0 - 100%
    val isTyping: Boolean = false,
    val ipAddress: String? = null
)

enum class ConnectionType {
    INTERNET_CLOUD,
    WIFI_LOCAL,
    WIFI_DIRECT,
    BLUETOOTH
}

/**
 * Model pesan teks dan multi-media real-time.
 */
@Entity(tableName = "sync_messages")
data class SyncMessage(
    @PrimaryKey val id: String,
    val senderNip: String,
    val senderName: String,
    val receiverNip: String,
    val messageText: String,
    val fileUri: String? = null,
    val fileType: FileType = FileType.TEXT,
    val fileName: String? = null,
    val fileSize: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val durationSeconds: Int? = null, // Untuk pesan suara (voice note)
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val connectionMethod: ConnectionType = ConnectionType.INTERNET_CLOUD
)

enum class FileType {
    TEXT,
    PHOTO,
    VIDEO,
    DOCUMENT,
    LOCATION,
    VOICE
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

/**
 * Log sinkronisasi data cloud / lokal.
 */
@Entity(tableName = "sync_history_logs")
data class SyncHistoryLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val entityType: String, // "Siswa", "Kelas", "Rencana Pembelajaran", "Jurnal"
    val actionType: String, // "INSERT", "UPDATE", "DELETE"
    val dataId: String,
    val syncStatus: String, // "SUCCESS", "OFFLINE_QUEUED", "FAILED"
    val peerDeviceName: String? = null
)
