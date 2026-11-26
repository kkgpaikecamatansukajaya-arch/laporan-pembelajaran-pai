package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.models.*
import com.example.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.min

sealed interface SyncBannerState {
    data object Idle : SyncBannerState
    data object Syncing : SyncBannerState
    data class Synced(val lastSyncedAt: Long) : SyncBannerState
    data class OfflineQueued(val pendingCount: Int, val message: String) : SyncBannerState
    data class Error(val message: String, val pendingCount: Int) : SyncBannerState
}

class CloudSyncEngine(
    private val context: Context,
    private val db: AppDatabase,
    private val gateway: RemoteBackendGateway,
    private val scope: CoroutineScope
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val syncMutex = Mutex()
    private val _isNetworkConnected = MutableStateFlow(checkCurrentNetworkConnected())
    val isNetworkConnected: StateFlow<Boolean> = _isNetworkConnected.asStateFlow()

    private val _syncState = MutableStateFlow<SyncBannerState>(SyncBannerState.Idle)
    val syncState: StateFlow<SyncBannerState> = _syncState.asStateFlow()

    val pendingOperations: Flow<List<PendingSyncOperation>> = db.pendingSyncDao().observePending()
    val pendingCount: Flow<Int> = db.pendingSyncDao().observePendingCount()

    private val syncPrefs = context.getSharedPreferences("cloud_sync_meta", Context.MODE_PRIVATE)
    private var realtimeCollectorJob: Job? = null
    private var retryTickerJob: Job? = null
    private var networkCallbackRegistered = false

    internal fun setNetworkConnectedForTest(connected: Boolean) {
        _isNetworkConnected.value = connected
    }

    private fun markTableSeen(table: String) {
        val uid = gateway.currentUserId ?: "default"
        syncPrefs.edit().putBoolean("seen_${uid}_$table", true).apply()
    }

    private fun shouldReconcileDeletes(table: String, remoteCount: Int): Boolean {
        val uid = gateway.currentUserId ?: "default"
        val key = "seen_${uid}_$table"
        if (remoteCount > 0) {
            syncPrefs.edit().putBoolean(key, true).apply()
            return true
        }
        return syncPrefs.getBoolean(key, false)
    }

    init {
        registerConnectivityCallback()
    }

    private fun checkCurrentNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val active = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(active) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }

    private fun registerConnectivityCallback() {
        if (networkCallbackRegistered) return
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkConnected.value = true
                    scope.launch {
                        if (gateway.currentUserId != null) {
                            gateway.startRealtimeSync()
                            flushPendingQueue(forceRetryNow = true)
                            pullAllFromCloud()
                        }
                    }
                }

                override fun onLost(network: Network) {
                    _isNetworkConnected.value = checkCurrentNetworkConnected()
                }
            })
            networkCallbackRegistered = true
        } catch (t: Throwable) {
            Log.w("CloudSyncEngine", "NetworkCallback unavailable: ${t.message}")
        }
    }

    fun startForAuthenticatedUser() {
        if (!gateway.isConfigured) return
        if (realtimeCollectorJob?.isActive != true) {
            realtimeCollectorJob = scope.launch {
                gateway.realtimeEvents.collect { event ->
                    handleRealtimeEvent(event)
                }
            }
        }
        if (retryTickerJob?.isActive != true) {
            retryTickerJob = scope.launch {
                while (isActive) {
                    delay(15_000L)
                    if (_isNetworkConnected.value && gateway.currentUserId != null) {
                        val pending = db.pendingSyncDao().getDueOperations(System.currentTimeMillis())
                        if (pending.isNotEmpty()) {
                            flushPendingQueue(forceRetryNow = false)
                        }
                    }
                }
            }
        }
        scope.launch {
            gateway.startRealtimeSync()
            syncNow(forceRetry = true)
        }
    }

    fun stopOnSignOut() {
        realtimeCollectorJob?.cancel()
        retryTickerJob?.cancel()
        scope.launch {
            gateway.stopRealtimeSync()
        }
        _syncState.value = SyncBannerState.Idle
    }

    suspend fun syncNow(forceRetry: Boolean = true): Result<String> {
        if (!gateway.isConfigured) {
            return Result.failure(IllegalStateException("Supabase belum dikonfigurasi."))
        }
        if (gateway.currentUserId == null) {
            return Result.failure(IllegalStateException("Pengguna belum login."))
        }
        _syncState.value = SyncBannerState.Syncing
        val flushResult = flushPendingQueue(forceRetryNow = forceRetry)
        val pullResult = pullAllFromCloud()
        val remaining = db.pendingSyncDao().getAllPending().size
        return if (flushResult.isSuccess && pullResult.isSuccess) {
            _syncState.value = if (remaining == 0) {
                SyncBannerState.Synced(System.currentTimeMillis())
            } else {
                SyncBannerState.OfflineQueued(remaining, "$remaining perubahan menunggu antrean ulang")
            }
            Result.success("Sinkronisasi Supabase selesai")
        } else {
            val err = pullResult.exceptionOrNull()?.localizedMessage
                ?: flushResult.exceptionOrNull()?.localizedMessage
                ?: "Koneksi tidak stabil"
            _syncState.value = if (remaining > 0) {
                SyncBannerState.OfflineQueued(remaining, "Offline/tidak stabil: $remaining antrean tersimpan")
            } else {
                SyncBannerState.Error(err, remaining)
            }
            Result.failure(Exception(err))
        }
    }

    suspend fun enqueueUpsert(table: String, recordKey: String, payloadJson: String) {
        val existing = db.pendingSyncDao().findExisting(table, recordKey)
        val op = PendingSyncOperation(
            id = existing?.id ?: 0L,
            tableName = table,
            operation = "UPSERT",
            recordKey = recordKey,
            payloadJson = payloadJson,
            retryCount = 0,
            lastError = null,
            nextRetryAt = 0L,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        db.pendingSyncDao().insertOrUpdate(op)
        recordSyncLog(table, "UPSERT", recordKey, "QUEUED", "Antrean Lokal")

        if (gateway.isConfigured && gateway.currentUserId != null && _isNetworkConnected.value) {
            flushPendingQueue(forceRetryNow = true)
        } else {
            val count = db.pendingSyncDao().getAllPending().size
            _syncState.value = SyncBannerState.OfflineQueued(count, "Tersimpan lokal, menunggu koneksi")
        }
    }

    suspend fun enqueueDelete(table: String, recordKey: String) {
        val existing = db.pendingSyncDao().findExisting(table, recordKey)
        val op = PendingSyncOperation(
            id = existing?.id ?: 0L,
            tableName = table,
            operation = "DELETE",
            recordKey = recordKey,
            payloadJson = "",
            retryCount = 0,
            lastError = null,
            nextRetryAt = 0L,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        db.pendingSyncDao().insertOrUpdate(op)
        recordSyncLog(table, "DELETE", recordKey, "QUEUED", "Antrean Lokal")

        if (gateway.isConfigured && gateway.currentUserId != null && _isNetworkConnected.value) {
            flushPendingQueue(forceRetryNow = true)
        } else {
            val count = db.pendingSyncDao().getAllPending().size
            _syncState.value = SyncBannerState.OfflineQueued(count, "Penghapusan diantrekan offline")
        }
    }

    suspend fun flushPendingQueue(forceRetryNow: Boolean = false): Result<Int> = syncMutex.withLock {
        if (!gateway.isConfigured || gateway.currentUserId == null) {
            return@withLock Result.success(0)
        }
        val now = System.currentTimeMillis()
        val operations = if (forceRetryNow) {
            db.pendingSyncDao().getAllPending()
        } else {
            db.pendingSyncDao().getDueOperations(now)
        }
        if (operations.isEmpty()) return@withLock Result.success(0)

        var successCount = 0
        var lastError: Throwable? = null

        for (op in operations) {
            val result = when (op.operation) {
                "DELETE" -> gateway.deleteRecord(op.tableName, op.recordKey)
                else -> gateway.upsertRecord(op.tableName, op.payloadJson)
            }
            if (result.isSuccess) {
                db.pendingSyncDao().deleteById(op.id)
                recordSyncLog(op.tableName, op.operation, op.recordKey, "SUCCESS", "Supabase Cloud")
                successCount++
            } else {
                val err = result.exceptionOrNull()
                lastError = err
                val nextAttempt = op.retryCount + 1
                val backoffMs = min(120_000L, 2_000L * (1L shl min(nextAttempt, 6)))
                db.pendingSyncDao().insertOrUpdate(
                    op.copy(
                        retryCount = nextAttempt,
                        lastError = err?.localizedMessage ?: "Koneksi gagal",
                        nextRetryAt = System.currentTimeMillis() + backoffMs
                    )
                )
                recordSyncLog(op.tableName, op.operation, op.recordKey, "OFFLINE_QUEUED", "Percobaan ulang #$nextAttempt")
            }
        }

        val remaining = db.pendingSyncDao().getAllPending().size
        _syncState.value = if (remaining == 0) {
            SyncBannerState.Synced(System.currentTimeMillis())
        } else {
            SyncBannerState.OfflineQueued(remaining, lastError?.localizedMessage ?: "Menunggu percobaan ulang ($remaining)")
        }

        return@withLock if (lastError == null || remaining == 0) {
            Result.success(successCount)
        } else {
            Result.failure(lastError)
        }
    }

    suspend fun pullAllFromCloud(): Result<Unit> = syncMutex.withLock {
        if (!gateway.isConfigured || gateway.currentUserId == null) {
            return@withLock Result.failure(IllegalStateException("Belum login atau belum terkonfigurasi"))
        }
        val snapshotResult = gateway.pullSnapshot()
        if (snapshotResult.isFailure) {
            return@withLock Result.failure(snapshotResult.exceptionOrNull() ?: Exception("Gagal mengambil data"))
        }
        val snapshot = snapshotResult.getOrThrow()
        applySnapshotToRoom(snapshot)
        recordSyncLog("Seluruh Database", "PULL_SYNC", "Supabase -> Room", "SUCCESS", "Supabase Realtime Cloud")
        Result.success(Unit)
    }

    private suspend fun applySnapshotToRoom(snapshot: RemoteDatabaseSnapshot) {
        val pendingKeys = db.pendingSyncDao().getAllPending().map { "${it.tableName}:${it.recordKey}" }.toSet()

        if (snapshot.profiles.isNotEmpty()) {
            db.managedUserDao().upsertAll(snapshot.profiles.map { it.toManagedUser() })
        }

        snapshot.schoolData?.let { dto ->
            if ("${SupabaseTables.SCHOOL_DATA}:1" !in pendingKeys) {
                db.schoolDataDao().insertOrUpdateSchoolData(dto.toEntity())
            }
        }

        snapshot.teacherProfile?.let { dto ->
            if ("${SupabaseTables.TEACHER_PROFILES}:1" !in pendingKeys) {
                db.teacherProfileDao().insertOrUpdateProfile(dto.toEntity())
            }
        }

        // Classes (preserve local id when syncKey matches)
        val classSyncKeyToLocalId = mutableMapOf<String, Int>()
        snapshot.classRooms.forEach { dto ->
            if ("${SupabaseTables.CLASS_ROOMS}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.classRoomDao().getClassBySyncKey(dto.syncKey)
                    ?: db.classRoomDao().getClassByName(dto.name)
                if (existing != null) {
                    db.classRoomDao().updateClass(existing.copy(name = dto.name, level = dto.level, syncKey = dto.syncKey))
                    classSyncKeyToLocalId[dto.syncKey] = existing.id
                } else {
                    val insertedId = db.classRoomDao().insertClass(
                        ClassRoom(name = dto.name, level = dto.level, syncKey = dto.syncKey)
                    ).toInt()
                    classSyncKeyToLocalId[dto.syncKey] = insertedId
                }
            }
        }
        // Populate map for all existing local classes
        db.classRoomDao().getAllClassesList().forEach {
            classSyncKeyToLocalId.putIfAbsent(it.syncKey, it.id)
        }

        // Students
        snapshot.students.forEach { dto ->
            if ("${SupabaseTables.STUDENTS}:${dto.syncKey}" !in pendingKeys) {
                val resolvedClassId = classSyncKeyToLocalId[dto.classSyncKey] ?: dto.classId.toInt()
                val existing = db.studentDao().getStudentBySyncKey(dto.syncKey)
                val entity = Student(
                    id = existing?.id ?: 0,
                    classId = resolvedClassId,
                    nis = dto.nis,
                    name = dto.name,
                    gender = dto.gender,
                    parentName = dto.parentName,
                    parentPhone = dto.parentPhone,
                    nisn = dto.nisn,
                    birthPlace = dto.birthPlace,
                    birthDate = dto.birthDate,
                    address = dto.address,
                    classSyncKey = dto.classSyncKey,
                    syncKey = dto.syncKey
                )
                db.studentDao().insertStudent(entity)
            }
        }

        // Merdeka documents
        snapshot.merdekaDocuments.forEach { dto ->
            if ("${SupabaseTables.MERDEKA_DOCUMENTS}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.merdekaDocumentDao().getDocumentBySyncKey(dto.syncKey)
                db.merdekaDocumentDao().insertDocument(
                    MerdekaDocument(
                        id = existing?.id ?: 0,
                        type = dto.type,
                        title = dto.title,
                        subject = dto.subject,
                        classLevel = dto.classLevel,
                        contentText = dto.contentText,
                        createdAt = dto.createdAt,
                        fileUri = dto.fileUri,
                        fileName = dto.fileName,
                        fileSize = dto.fileSize,
                        fileMimeType = dto.fileMimeType,
                        semester = dto.semester,
                        academicYear = dto.academicYear,
                        authorTeacher = dto.authorTeacher,
                        storagePath = dto.storagePath,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Journals
        snapshot.teachingJournals.forEach { dto ->
            if ("${SupabaseTables.TEACHING_JOURNALS}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.teachingJournalDao().getBySyncKey(dto.syncKey)
                db.teachingJournalDao().insertJournal(
                    TeachingJournal(
                        id = existing?.id ?: 0,
                        date = dto.date,
                        classId = dto.classId.toInt(),
                        className = dto.className,
                        topic = dto.topic,
                        activity = dto.activity,
                        teacherPresent = dto.teacherPresent,
                        studentPresenceJson = dto.studentPresenceJson,
                        locationGps = dto.locationGps,
                        photoPath = dto.photoPath,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Assessments
        snapshot.assessmentRecords.forEach { dto ->
            if ("${SupabaseTables.ASSESSMENT_RECORDS}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.assessmentRecordDao().getBySyncKey(dto.syncKey)
                db.assessmentRecordDao().insertAssessment(
                    AssessmentRecord(
                        id = existing?.id ?: 0,
                        studentId = dto.studentId.toInt(),
                        studentName = dto.studentName,
                        classId = dto.classId.toInt(),
                        type = dto.type,
                        topic = dto.topic,
                        score = dto.score,
                        description = dto.description,
                        date = dto.date,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Questions
        snapshot.questions.forEach { dto ->
            if ("${SupabaseTables.QUESTIONS}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.questionDao().getBySyncKey(dto.syncKey)
                db.questionDao().insertQuestion(
                    Question(
                        id = existing?.id ?: 0,
                        text = dto.text,
                        optionA = dto.optionA,
                        optionB = dto.optionB,
                        optionC = dto.optionC,
                        optionD = dto.optionD,
                        answerKey = dto.answerKey,
                        difficulty = dto.difficulty,
                        type = dto.type,
                        explanation = dto.explanation,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Religious activities
        snapshot.religiousActivities.forEach { dto ->
            if ("${SupabaseTables.RELIGIOUS_ACTIVITIES}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.religiousActivityDao().getBySyncKey(dto.syncKey)
                db.religiousActivityDao().insertActivity(
                    ReligiousActivity(
                        id = existing?.id ?: 0,
                        title = dto.title,
                        date = dto.date,
                        description = dto.description,
                        type = dto.type,
                        participantCount = dto.participantCount,
                        photoPath = dto.photoPath,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Extracurriculars
        snapshot.extracurriculars.forEach { dto ->
            if ("${SupabaseTables.EXTRACURRICULARS}:${dto.syncKey}" !in pendingKeys) {
                val existing = db.extracurricularDao().getBySyncKey(dto.syncKey)
                db.extracurricularDao().insertExtracurricular(
                    Extracurricular(
                        id = existing?.id ?: 0,
                        title = dto.title,
                        mentorName = dto.mentorName,
                        schedule = dto.schedule,
                        participantsCount = dto.participantsCount,
                        description = dto.description,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Attendance, Hafalan, Messages, Archives, ActivityLogs (UUID/text PKs)
        val attendanceToUpsert = snapshot.attendanceRecords
            .filter { "${SupabaseTables.ATTENDANCE_RECORDS}:${it.id}" !in pendingKeys }
            .map { it.toEntity() }
        if (attendanceToUpsert.isNotEmpty()) db.attendanceDao().upsertAll(attendanceToUpsert)

        val hafalanToUpsert = snapshot.hafalanRecords
            .filter { "${SupabaseTables.HAFALAN_RECORDS}:${it.id}" !in pendingKeys }
            .map { it.toEntity() }
        if (hafalanToUpsert.isNotEmpty()) db.hafalanDao().upsertAll(hafalanToUpsert)

        val messagesToUpsert = snapshot.syncMessages
            .filter { "${SupabaseTables.SYNC_MESSAGES}:${it.id}" !in pendingKeys }
            .map { it.toEntity() }
        if (messagesToUpsert.isNotEmpty()) db.syncMessageDao().upsertAll(messagesToUpsert)

        val archivesToUpsert = snapshot.digitalArchives
            .filter { "${SupabaseTables.DIGITAL_ARCHIVES}:${it.id}" !in pendingKeys }
            .map { it.toEntity() }
        if (archivesToUpsert.isNotEmpty()) db.digitalArchiveDao().upsertAll(archivesToUpsert)

        snapshot.activityLogs.forEach { dto ->
            if (db.activityLogDao().getBySyncKey(dto.syncKey) == null) {
                db.activityLogDao().insertLog(
                    ActivityLog(
                        timestamp = dto.timestamp,
                        text = dto.text,
                        userType = dto.userType,
                        userEmail = dto.userEmail,
                        userName = dto.userName,
                        deviceInfo = dto.deviceInfo,
                        syncKey = dto.syncKey
                    )
                )
            }
        }

        // Reconcile remote deletes on pull (e.g. when device reopens after another device deleted rows)
        if (shouldReconcileDeletes(SupabaseTables.CLASS_ROOMS, snapshot.classRooms.size)) {
            val remoteKeys = snapshot.classRooms.map { it.syncKey }.toSet()
            db.classRoomDao().getAllClassesList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.CLASS_ROOMS}:${local.syncKey}" !in pendingKeys) {
                    db.classRoomDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.STUDENTS, snapshot.students.size)) {
            val remoteKeys = snapshot.students.map { it.syncKey }.toSet()
            db.studentDao().getAllStudentsList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.STUDENTS}:${local.syncKey}" !in pendingKeys) {
                    db.studentDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.MERDEKA_DOCUMENTS, snapshot.merdekaDocuments.size)) {
            val remoteKeys = snapshot.merdekaDocuments.map { it.syncKey }.toSet()
            db.merdekaDocumentDao().getAllDocumentsList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.MERDEKA_DOCUMENTS}:${local.syncKey}" !in pendingKeys) {
                    db.merdekaDocumentDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.TEACHING_JOURNALS, snapshot.teachingJournals.size)) {
            val remoteKeys = snapshot.teachingJournals.map { it.syncKey }.toSet()
            db.teachingJournalDao().getAllJournalsList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.TEACHING_JOURNALS}:${local.syncKey}" !in pendingKeys) {
                    db.teachingJournalDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.ASSESSMENT_RECORDS, snapshot.assessmentRecords.size)) {
            val remoteKeys = snapshot.assessmentRecords.map { it.syncKey }.toSet()
            db.assessmentRecordDao().getAllAssessmentsList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.ASSESSMENT_RECORDS}:${local.syncKey}" !in pendingKeys) {
                    db.assessmentRecordDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.QUESTIONS, snapshot.questions.size)) {
            val remoteKeys = snapshot.questions.map { it.syncKey }.toSet()
            db.questionDao().getAllQuestionsList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.QUESTIONS}:${local.syncKey}" !in pendingKeys) {
                    db.questionDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.RELIGIOUS_ACTIVITIES, snapshot.religiousActivities.size)) {
            val remoteKeys = snapshot.religiousActivities.map { it.syncKey }.toSet()
            db.religiousActivityDao().getAllActivitiesList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.RELIGIOUS_ACTIVITIES}:${local.syncKey}" !in pendingKeys) {
                    db.religiousActivityDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.EXTRACURRICULARS, snapshot.extracurriculars.size)) {
            val remoteKeys = snapshot.extracurriculars.map { it.syncKey }.toSet()
            db.extracurricularDao().getAllExtracurricularsList().forEach { local ->
                if (local.syncKey !in remoteKeys && "${SupabaseTables.EXTRACURRICULARS}:${local.syncKey}" !in pendingKeys) {
                    db.extracurricularDao().deleteBySyncKey(local.syncKey)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.ATTENDANCE_RECORDS, snapshot.attendanceRecords.size)) {
            val remoteIds = snapshot.attendanceRecords.map { it.id }.toSet()
            db.attendanceDao().getAllAttendanceList().forEach { local ->
                if (local.id !in remoteIds && "${SupabaseTables.ATTENDANCE_RECORDS}:${local.id}" !in pendingKeys) {
                    db.attendanceDao().deleteById(local.id)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.HAFALAN_RECORDS, snapshot.hafalanRecords.size)) {
            val remoteIds = snapshot.hafalanRecords.map { it.id }.toSet()
            db.hafalanDao().getAllHafalanList().forEach { local ->
                if (local.id !in remoteIds && "${SupabaseTables.HAFALAN_RECORDS}:${local.id}" !in pendingKeys) {
                    db.hafalanDao().deleteById(local.id)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.DIGITAL_ARCHIVES, snapshot.digitalArchives.size)) {
            val remoteIds = snapshot.digitalArchives.map { it.id }.toSet()
            db.digitalArchiveDao().getAllArchivesList().forEach { local ->
                if (local.id !in remoteIds && "${SupabaseTables.DIGITAL_ARCHIVES}:${local.id}" !in pendingKeys) {
                    db.digitalArchiveDao().deleteById(local.id)
                }
            }
        }
        if (shouldReconcileDeletes(SupabaseTables.SYNC_MESSAGES, snapshot.syncMessages.size)) {
            val remoteIds = snapshot.syncMessages.map { it.id }.toSet()
            db.syncMessageDao().getAllMessagesList().forEach { local ->
                if (local.id !in remoteIds && "${SupabaseTables.SYNC_MESSAGES}:${local.id}" !in pendingKeys) {
                    db.syncMessageDao().deleteById(local.id)
                }
            }
        }
    }

    private suspend fun handleRealtimeEvent(event: RealtimeTableEvent) {
        try {
            if (event.actionType == "DELETE" && !event.recordKey.isNullOrBlank()) {
                when (event.table) {
                    SupabaseTables.CLASS_ROOMS -> db.classRoomDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.STUDENTS -> db.studentDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.MERDEKA_DOCUMENTS -> db.merdekaDocumentDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.TEACHING_JOURNALS -> db.teachingJournalDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.ASSESSMENT_RECORDS -> db.assessmentRecordDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.QUESTIONS -> db.questionDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.RELIGIOUS_ACTIVITIES -> db.religiousActivityDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.EXTRACURRICULARS -> db.extracurricularDao().deleteBySyncKey(event.recordKey)
                    SupabaseTables.ATTENDANCE_RECORDS -> db.attendanceDao().deleteById(event.recordKey)
                    SupabaseTables.HAFALAN_RECORDS -> db.hafalanDao().deleteById(event.recordKey)
                    SupabaseTables.SYNC_MESSAGES -> db.syncMessageDao().deleteById(event.recordKey)
                    SupabaseTables.DIGITAL_ARCHIVES -> db.digitalArchiveDao().deleteById(event.recordKey)
                    SupabaseTables.PROFILES -> db.managedUserDao().deleteById(event.recordKey)
                }
                recordSyncLog(event.table, "REALTIME_DELETE", event.recordKey, "SUCCESS", "Perangkat Lain")
                markTableSeen(event.table)
                return
            }

            // For INSERT/UPDATE, apply fast incremental decode when payloadJson is available, or pull snapshot
            val payload = event.payloadJson
            if (!payload.isNullOrBlank()) {
                val handled = tryApplyIncrementalPayload(event.table, payload)
                if (handled) {
                    markTableSeen(event.table)
                    recordSyncLog(event.table, "REALTIME_${event.actionType}", event.recordKey ?: "-", "SUCCESS", "Perangkat Lain")
                    _syncState.value = SyncBannerState.Synced(System.currentTimeMillis())
                    return
                }
            }
            pullAllFromCloud()
        } catch (e: Exception) {
            Log.w("CloudSyncEngine", "Realtime event handling fallback: ${e.message}")
            pullAllFromCloud()
        }
    }

    private suspend fun tryApplyIncrementalPayload(table: String, payload: String): Boolean {
        return runCatching {
            when (table) {
                SupabaseTables.PROFILES -> {
                    db.managedUserDao().upsert(json.decodeFromString<SupabaseProfileDto>(payload).toManagedUser())
                    true
                }
                SupabaseTables.CLASS_ROOMS -> {
                    val dto = json.decodeFromString<SupabaseClassRoomDto>(payload)
                    val existing = db.classRoomDao().getClassBySyncKey(dto.syncKey)
                    if (existing != null) {
                        db.classRoomDao().updateClass(existing.copy(name = dto.name, level = dto.level))
                    } else {
                        db.classRoomDao().insertClass(ClassRoom(name = dto.name, level = dto.level, syncKey = dto.syncKey))
                    }
                    true
                }
                SupabaseTables.STUDENTS -> {
                    val dto = json.decodeFromString<SupabaseStudentDto>(payload)
                    val existing = db.studentDao().getStudentBySyncKey(dto.syncKey)
                    val classId = db.classRoomDao().getClassBySyncKey(dto.classSyncKey)?.id ?: dto.classId.toInt()
                    db.studentDao().insertStudent(
                        Student(
                            id = existing?.id ?: 0,
                            classId = classId,
                            nis = dto.nis,
                            name = dto.name,
                            gender = dto.gender,
                            parentName = dto.parentName,
                            parentPhone = dto.parentPhone,
                            nisn = dto.nisn,
                            birthPlace = dto.birthPlace,
                            birthDate = dto.birthDate,
                            address = dto.address,
                            classSyncKey = dto.classSyncKey,
                            syncKey = dto.syncKey
                        )
                    )
                    true
                }
                SupabaseTables.ATTENDANCE_RECORDS -> {
                    db.attendanceDao().upsert(json.decodeFromString<SupabaseAttendanceRecordDto>(payload).toEntity())
                    true
                }
                SupabaseTables.HAFALAN_RECORDS -> {
                    db.hafalanDao().upsert(json.decodeFromString<SupabaseHafalanRecordDto>(payload).toEntity())
                    true
                }
                SupabaseTables.SYNC_MESSAGES -> {
                    db.syncMessageDao().upsert(json.decodeFromString<SupabaseSyncMessageDto>(payload).toEntity())
                    true
                }
                SupabaseTables.DIGITAL_ARCHIVES -> {
                    db.digitalArchiveDao().upsert(json.decodeFromString<SupabaseDigitalArchiveDto>(payload).toEntity())
                    true
                }
                SupabaseTables.ACTIVITY_LOGS -> {
                    val dto = json.decodeFromString<SupabaseActivityLogDto>(payload)
                    if (db.activityLogDao().getBySyncKey(dto.syncKey) == null) {
                        db.activityLogDao().insertLog(
                            ActivityLog(
                                timestamp = dto.timestamp,
                                text = dto.text,
                                userType = dto.userType,
                                userEmail = dto.userEmail,
                                userName = dto.userName,
                                deviceInfo = dto.deviceInfo,
                                syncKey = dto.syncKey
                            )
                        )
                    }
                    true
                }
                else -> false
            }
        }.getOrDefault(false)
    }

    suspend fun pushLocalBootstrapIfRemoteEmpty() {
        if (!gateway.isConfigured || gateway.currentUserId == null) return
        val remote = gateway.pullSnapshot().getOrNull() ?: return
        val uid = gateway.currentUserId

        if (remote.schoolData == null) {
            db.schoolDataDao().getSchoolDataOnce()?.let { school ->
                enqueueUpsert(SupabaseTables.SCHOOL_DATA, "1", encodeSchoolData(school, uid))
            }
        }
        if (remote.teacherProfile == null) {
            db.teacherProfileDao().getProfileOnce()?.let { profile ->
                enqueueUpsert(SupabaseTables.TEACHER_PROFILES, "1", encodeTeacherProfile(profile, uid))
            }
        }
        if (remote.classRooms.isEmpty()) {
            db.classRoomDao().getAllClassesList().forEach { cls ->
                enqueueUpsert(SupabaseTables.CLASS_ROOMS, cls.syncKey, encodeClassRoom(cls, uid))
            }
        }
        if (remote.students.isEmpty()) {
            val classesById = db.classRoomDao().getAllClassesList().associateBy { it.id }
            db.studentDao().getAllStudentsList().forEach { student ->
                val clsSyncKey = student.classSyncKey.ifBlank { classesById[student.classId]?.syncKey.orEmpty() }
                enqueueUpsert(
                    SupabaseTables.STUDENTS,
                    student.syncKey,
                    encodeStudent(student.copy(classSyncKey = clsSyncKey), uid)
                )
            }
        }
        if (remote.merdekaDocuments.isEmpty()) {
            db.merdekaDocumentDao().getAllDocumentsList().forEach { doc ->
                enqueueUpsert(SupabaseTables.MERDEKA_DOCUMENTS, doc.syncKey, encodeDocument(doc, uid))
            }
        }
        if (remote.questions.isEmpty()) {
            db.questionDao().getAllQuestionsList().forEach { q ->
                enqueueUpsert(SupabaseTables.QUESTIONS, q.syncKey, encodeQuestion(q, uid))
            }
        }
        if (remote.extracurriculars.isEmpty()) {
            db.extracurricularDao().getAllExtracurricularsList().forEach { ex ->
                enqueueUpsert(SupabaseTables.EXTRACURRICULARS, ex.syncKey, encodeExtracurricular(ex, uid))
            }
        }
        flushPendingQueue(forceRetryNow = true)
    }

    private suspend fun recordSyncLog(entity: String, action: String, dataId: String, status: String, peer: String?) {
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

    // --- JSON Encoders for Queueing ---
    fun encodeSchoolData(s: SchoolData, uid: String?): String = json.encodeToString(
        SupabaseSchoolDataDto(1L, s.schoolName, s.npsn, s.address, s.headmasterName, s.academicYear, s.semester, s.curriculum, uid)
    )

    fun encodeTeacherProfile(t: TeacherProfile, uid: String?): String = json.encodeToString(
        SupabaseTeacherProfileDto(1L, uid, t.fullName, t.nip, t.nuptk, t.certNo, t.rank, t.education, t.email, t.phone, t.employmentStatus, t.subjectTaught, t.workUnit, t.additionalDuties, t.birthPlaceDate)
    )

    fun encodeClassRoom(c: ClassRoom, uid: String?): String = json.encodeToString(
        SupabaseClassRoomDto(syncKey = c.syncKey, name = c.name, level = c.level, createdBy = uid)
    )

    fun encodeStudent(s: Student, uid: String?): String = json.encodeToString(
        SupabaseStudentDto(
            syncKey = s.syncKey,
            classId = s.classId.toLong(),
            classSyncKey = s.classSyncKey,
            nis = s.nis,
            name = s.name,
            gender = s.gender,
            parentName = s.parentName,
            parentPhone = s.parentPhone,
            nisn = s.nisn,
            birthPlace = s.birthPlace,
            birthDate = s.birthDate,
            address = s.address,
            createdBy = uid
        )
    )

    fun encodeDocument(d: MerdekaDocument, uid: String?): String = json.encodeToString(
        SupabaseMerdekaDocumentDto(
            syncKey = d.syncKey,
            type = d.type,
            title = d.title,
            subject = d.subject,
            classLevel = d.classLevel,
            contentText = d.contentText,
            createdAt = d.createdAt,
            fileUri = d.fileUri,
            fileName = d.fileName,
            fileSize = d.fileSize,
            fileMimeType = d.fileMimeType,
            storagePath = d.storagePath,
            semester = d.semester,
            academicYear = d.academicYear,
            authorTeacher = d.authorTeacher,
            createdBy = uid
        )
    )

    fun encodeJournal(j: TeachingJournal, uid: String?): String = json.encodeToString(
        SupabaseTeachingJournalDto(
            syncKey = j.syncKey,
            date = j.date,
            classId = j.classId.toLong(),
            className = j.className,
            topic = j.topic,
            activity = j.activity,
            teacherPresent = j.teacherPresent,
            studentPresenceJson = j.studentPresenceJson,
            locationGps = j.locationGps,
            photoPath = j.photoPath,
            createdBy = uid
        )
    )

    fun encodeAssessment(a: AssessmentRecord, uid: String?): String = json.encodeToString(
        SupabaseAssessmentRecordDto(
            syncKey = a.syncKey,
            studentId = a.studentId.toLong(),
            studentName = a.studentName,
            classId = a.classId.toLong(),
            type = a.type,
            topic = a.topic,
            score = a.score,
            description = a.description,
            date = a.date,
            createdBy = uid
        )
    )

    fun encodeQuestion(q: Question, uid: String?): String = json.encodeToString(
        SupabaseQuestionDto(
            syncKey = q.syncKey,
            text = q.text,
            optionA = q.optionA,
            optionB = q.optionB,
            optionC = q.optionC,
            optionD = q.optionD,
            answerKey = q.answerKey,
            difficulty = q.difficulty,
            type = q.type,
            explanation = q.explanation,
            createdBy = uid
        )
    )

    fun encodeReligiousActivity(r: ReligiousActivity, uid: String?): String = json.encodeToString(
        SupabaseReligiousActivityDto(
            syncKey = r.syncKey,
            title = r.title,
            date = r.date,
            description = r.description,
            type = r.type,
            participantCount = r.participantCount,
            photoPath = r.photoPath,
            createdBy = uid
        )
    )

    fun encodeExtracurricular(e: Extracurricular, uid: String?): String = json.encodeToString(
        SupabaseExtracurricularDto(
            syncKey = e.syncKey,
            title = e.title,
            mentorName = e.mentorName,
            schedule = e.schedule,
            participantsCount = e.participantsCount,
            description = e.description,
            createdBy = uid
        )
    )

    fun encodeAttendance(a: AttendanceRecord, uid: String?): String = json.encodeToString(
        SupabaseAttendanceRecordDto(
            id = a.id,
            date = a.date,
            classId = a.classId.toLong(),
            className = a.className,
            studentId = a.studentId.toLong(),
            studentName = a.studentName,
            gender = a.gender,
            status = a.status.name,
            notes = a.notes,
            subject = a.subject,
            createdBy = uid
        )
    )

    fun encodeHafalan(h: HafalanRecord, uid: String?): String = json.encodeToString(
        SupabaseHafalanRecordDto(
            id = h.id,
            studentId = h.studentId.toLong(),
            studentName = h.studentName,
            classId = h.classId.toLong(),
            className = h.className,
            surahNumber = h.surahNumber,
            surahName = h.surahName,
            date = h.date,
            score = h.score,
            status = h.status.name,
            kelancaran = h.kelancaran,
            tajwid = h.tajwid,
            makhraj = h.makhraj,
            testerTeacher = h.testerTeacher,
            notes = h.notes,
            createdBy = uid
        )
    )

    fun encodeSyncMessage(m: SyncMessage, uid: String?): String = json.encodeToString(
        SupabaseSyncMessageDto(
            id = m.id,
            senderId = uid,
            senderNip = m.senderNip,
            senderName = m.senderName,
            receiverNip = m.receiverNip,
            messageText = m.messageText,
            fileUri = m.fileUri,
            fileType = m.fileType.name,
            fileName = m.fileName,
            fileSize = m.fileSize,
            locationLatitude = m.locationLatitude,
            locationLongitude = m.locationLongitude,
            durationSeconds = m.durationSeconds,
            timestamp = m.timestamp,
            status = m.status.name,
            connectionMethod = m.connectionMethod.name
        )
    )

    fun encodeDigitalArchive(a: DigitalArchiveItem, uid: String?): String = json.encodeToString(
        SupabaseDigitalArchiveDto(
            id = a.id,
            title = a.title,
            category = a.category,
            fileName = a.fileName,
            fileSize = a.fileSize,
            mimeType = a.mimeType,
            storagePath = a.storagePath,
            publicUrl = a.publicUrl,
            uploadedBy = uid,
            uploadedByName = a.uploadedByName,
            createdAt = a.createdAt
        )
    )

    fun encodeActivityLog(log: ActivityLog, uid: String?): String = json.encodeToString(
        SupabaseActivityLogDto(
            syncKey = log.syncKey,
            userId = uid,
            userEmail = log.userEmail,
            userName = log.userName,
            userType = log.userType,
            text = log.text,
            deviceInfo = log.deviceInfo,
            timestamp = log.timestamp
        )
    )
}
