package com.example.data.remote

import android.util.Log
import com.example.data.models.AppRole
import com.example.data.models.ManagedUserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.hours

class SupabaseBackendGateway(
    private val clientProvider: () -> SupabaseClient? = { SupabaseClientProvider.getClient() }
) : RemoteBackendGateway {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val realtimeMutex = Mutex()
    private var activeChannel: RealtimeChannel? = null
    private val realtimeJobs = mutableListOf<Job>()

    private val _realtimeEvents = MutableSharedFlow<RealtimeTableEvent>(extraBufferCapacity = 64)
    override val realtimeEvents: Flow<RealtimeTableEvent> = _realtimeEvents.asSharedFlow()

    override val isConfigured: Boolean
        get() = clientProvider() != null

    override val currentUserId: String?
        get() = clientProvider()?.auth?.currentUserOrNull()?.id

    private fun requireClient(): SupabaseClient {
        return clientProvider()
            ?: throw IllegalStateException(
                "SUPABASE_PUBLISHABLE_KEY belum dikonfigurasi pada Secrets panel AI Studio."
            )
    }

    override suspend fun restoreSession(): Result<ManagedUserProfile?> = runCatching {
        val client = clientProvider() ?: return@runCatching null
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: return@runCatching null
        fetchOrBuildProfile(client, user.id, user.email.orEmpty())
    }

    override suspend fun signIn(email: String, password: String): Result<ManagedUserProfile> = runCatching {
        val cleanEmail = email.trim()
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Format email tidak valid." }
        require(password.length >= 6) { "Kata sandi minimal 6 karakter." }

        val client = requireClient()
        client.auth.signInWith(Email) {
            this.email = cleanEmail
            this.password = password
        }
        val user = client.auth.currentUserOrNull()
            ?: throw IllegalStateException("Sesi login Supabase tidak ditemukan.")
        val profile = fetchOrBuildProfile(client, user.id, user.email ?: cleanEmail)
        if (!profile.isActive) {
            runCatching { client.auth.signOut() }
            throw IllegalStateException("Akun Anda dinonaktifkan oleh Admin. Hubungi Super Admin KKG PAI.")
        }
        profile
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        nip: String,
        schoolName: String
    ): Result<SignUpOutcome> = runCatching {
        val cleanEmail = email.trim()
        val cleanName = fullName.trim()
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Format email tidak valid." }
        require(password.length >= 6) { "Kata sandi minimal 6 karakter." }
        require(cleanName.length >= 3) { "Nama lengkap minimal 3 karakter." }

        val client = requireClient()
        client.auth.signUpWith(Email) {
            this.email = cleanEmail
            this.password = password
            data = buildJsonObject {
                put("full_name", cleanName)
                put("nip", nip.trim())
                put("school_name", schoolName.trim().ifBlank { "SD Negeri Kiarapandak 01" })
            }
        }
        val user = client.auth.currentUserOrNull()
        val session = client.auth.currentSessionOrNull()
        if (user != null && session != null) {
            val profile = fetchOrBuildProfile(client, user.id, user.email ?: cleanEmail)
            SignUpOutcome.Authenticated(profile)
        } else {
            SignUpOutcome.EmailConfirmationRequired(cleanEmail)
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        stopRealtimeSync()
        clientProvider()?.auth?.signOut()
        Unit
    }

    private suspend fun fetchOrBuildProfile(
        client: SupabaseClient,
        userId: String,
        email: String
    ): ManagedUserProfile {
        val dto = runCatching {
            client.from(SupabaseTables.PROFILES)
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<SupabaseProfileDto>()
        }.getOrNull()

        return dto?.toManagedUser() ?: ManagedUserProfile(
            id = userId,
            email = email,
            fullName = email.substringBefore("@"),
            role = if (email.equals("kkgpaikecamatansukajaya@gmail.com", ignoreCase = true)) {
                AppRole.SUPER_ADMIN.dbValue
            } else {
                AppRole.SISWA.dbValue
            },
            isActive = true
        )
    }

    override suspend fun fetchAllProfiles(): Result<List<ManagedUserProfile>> = runCatching {
        val client = requireClient()
        client.from(SupabaseTables.PROFILES)
            .select {
                order("full_name", Order.ASCENDING)
            }
            .decodeList<SupabaseProfileDto>()
            .map { it.toManagedUser() }
    }

    override suspend fun updateUserRoleAndStatus(
        userId: String,
        role: AppRole,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        val client = requireClient()
        client.from(SupabaseTables.PROFILES).update({
            set("role", role.dbValue)
            set("is_active", isActive)
        }) {
            filter { eq("id", userId) }
        }
        Unit
    }

    override suspend fun pullSnapshot(): Result<RemoteDatabaseSnapshot> = runCatching {
        val client = requireClient()
        val profiles = runCatching {
            client.from(SupabaseTables.PROFILES).select().decodeList<SupabaseProfileDto>()
        }.getOrDefault(emptyList())

        val school = runCatching {
            client.from(SupabaseTables.SCHOOL_DATA)
                .select { filter { eq("id", 1L) } }
                .decodeSingleOrNull<SupabaseSchoolDataDto>()
        }.getOrNull()

        val teacher = runCatching {
            client.from(SupabaseTables.TEACHER_PROFILES)
                .select { filter { eq("id", 1L) } }
                .decodeSingleOrNull<SupabaseTeacherProfileDto>()
        }.getOrNull()

        val classes = client.from(SupabaseTables.CLASS_ROOMS).select().decodeList<SupabaseClassRoomDto>()
        val students = client.from(SupabaseTables.STUDENTS).select().decodeList<SupabaseStudentDto>()
        val docs = client.from(SupabaseTables.MERDEKA_DOCUMENTS).select().decodeList<SupabaseMerdekaDocumentDto>()
        val journals = client.from(SupabaseTables.TEACHING_JOURNALS).select().decodeList<SupabaseTeachingJournalDto>()
        val assessments = client.from(SupabaseTables.ASSESSMENT_RECORDS).select().decodeList<SupabaseAssessmentRecordDto>()
        val questions = client.from(SupabaseTables.QUESTIONS).select().decodeList<SupabaseQuestionDto>()
        val activities = client.from(SupabaseTables.RELIGIOUS_ACTIVITIES).select().decodeList<SupabaseReligiousActivityDto>()
        val extras = client.from(SupabaseTables.EXTRACURRICULARS).select().decodeList<SupabaseExtracurricularDto>()
        val attendance = client.from(SupabaseTables.ATTENDANCE_RECORDS).select().decodeList<SupabaseAttendanceRecordDto>()
        val hafalan = client.from(SupabaseTables.HAFALAN_RECORDS).select().decodeList<SupabaseHafalanRecordDto>()
        val messages = client.from(SupabaseTables.SYNC_MESSAGES).select {
            order("timestamp", Order.ASCENDING)
        }.decodeList<SupabaseSyncMessageDto>()
        val archives = client.from(SupabaseTables.DIGITAL_ARCHIVES).select {
            order("created_at", Order.DESCENDING)
        }.decodeList<SupabaseDigitalArchiveDto>()
        val logs = runCatching {
            client.from(SupabaseTables.ACTIVITY_LOGS).select {
                order("timestamp", Order.DESCENDING)
                limit(100)
            }.decodeList<SupabaseActivityLogDto>()
        }.getOrDefault(emptyList())

        RemoteDatabaseSnapshot(
            profiles = profiles,
            schoolData = school,
            teacherProfile = teacher,
            classRooms = classes,
            students = students,
            merdekaDocuments = docs,
            teachingJournals = journals,
            assessmentRecords = assessments,
            questions = questions,
            religiousActivities = activities,
            extracurriculars = extras,
            attendanceRecords = attendance,
            hafalanRecords = hafalan,
            syncMessages = messages,
            digitalArchives = archives,
            activityLogs = logs
        )
    }

    override suspend fun upsertRecord(table: String, payloadJson: String): Result<Unit> = runCatching {
        val client = requireClient()
        when (table) {
            SupabaseTables.SCHOOL_DATA -> {
                val dto = json.decodeFromString<SupabaseSchoolDataDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "id" }
            }
            SupabaseTables.TEACHER_PROFILES -> {
                val dto = json.decodeFromString<SupabaseTeacherProfileDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "id" }
            }
            SupabaseTables.CLASS_ROOMS -> {
                val dto = json.decodeFromString<SupabaseClassRoomDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.STUDENTS -> {
                val dto = json.decodeFromString<SupabaseStudentDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.MERDEKA_DOCUMENTS -> {
                val dto = json.decodeFromString<SupabaseMerdekaDocumentDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.TEACHING_JOURNALS -> {
                val dto = json.decodeFromString<SupabaseTeachingJournalDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.ASSESSMENT_RECORDS -> {
                val dto = json.decodeFromString<SupabaseAssessmentRecordDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.QUESTIONS -> {
                val dto = json.decodeFromString<SupabaseQuestionDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.RELIGIOUS_ACTIVITIES -> {
                val dto = json.decodeFromString<SupabaseReligiousActivityDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.EXTRACURRICULARS -> {
                val dto = json.decodeFromString<SupabaseExtracurricularDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            SupabaseTables.ATTENDANCE_RECORDS -> {
                val dto = json.decodeFromString<SupabaseAttendanceRecordDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "id" }
            }
            SupabaseTables.HAFALAN_RECORDS -> {
                val dto = json.decodeFromString<SupabaseHafalanRecordDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "id" }
            }
            SupabaseTables.SYNC_MESSAGES -> {
                val dto = json.decodeFromString<SupabaseSyncMessageDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "id" }
            }
            SupabaseTables.DIGITAL_ARCHIVES -> {
                val dto = json.decodeFromString<SupabaseDigitalArchiveDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "id" }
            }
            SupabaseTables.ACTIVITY_LOGS -> {
                val dto = json.decodeFromString<SupabaseActivityLogDto>(payloadJson)
                client.from(table).upsert(dto) { onConflict = "sync_key" }
            }
            else -> throw IllegalArgumentException("Tabel tidak dikenal: $table")
        }
        Unit
    }

    override suspend fun deleteRecord(table: String, recordKey: String): Result<Unit> = runCatching {
        val client = requireClient()
        val keyColumn = when (table) {
            SupabaseTables.ATTENDANCE_RECORDS,
            SupabaseTables.HAFALAN_RECORDS,
            SupabaseTables.SYNC_MESSAGES,
            SupabaseTables.DIGITAL_ARCHIVES -> "id"
            else -> "sync_key"
        }
        client.from(table).delete {
            filter { eq(keyColumn, recordKey) }
        }
        Unit
    }

    override suspend fun startRealtimeSync() {
        val client = clientProvider() ?: return
        if (client.auth.currentUserOrNull() == null) return

        realtimeMutex.withLock {
            if (activeChannel != null) return
            try {
                val channel = client.channel("sim-pai-multi-device-sync")
                SupabaseTables.realtimeSyncedTables.forEach { tableName ->
                    val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = tableName
                    }
                    val job = flow.onEach { action ->
                        val event = when (action) {
                            is PostgresAction.Insert -> {
                                val key = extractRecordKey(action.record)
                                RealtimeTableEvent(tableName, "INSERT", key, action.record.toString())
                            }
                            is PostgresAction.Update -> {
                                val key = extractRecordKey(action.record)
                                RealtimeTableEvent(tableName, "UPDATE", key, action.record.toString())
                            }
                            is PostgresAction.Delete -> {
                                val key = extractRecordKey(action.oldRecord)
                                RealtimeTableEvent(tableName, "DELETE", key, action.oldRecord.toString())
                            }
                            is PostgresAction.Select -> null
                        }
                        if (event != null) {
                            _realtimeEvents.tryEmit(event)
                        }
                    }.launchIn(scope)
                    realtimeJobs.add(job)
                }
                channel.subscribe(blockUntilSubscribed = false)
                activeChannel = channel
            } catch (e: Exception) {
                Log.w("SupabaseBackend", "Realtime subscribe warning: ${e.message}")
            }
        }
    }

    override suspend fun stopRealtimeSync() {
        realtimeMutex.withLock {
            realtimeJobs.forEach { it.cancel() }
            realtimeJobs.clear()
            val channel = activeChannel
            activeChannel = null
            if (channel != null) {
                runCatching {
                    channel.unsubscribe()
                    clientProvider()?.realtime?.removeChannel(channel)
                }
            }
        }
    }

    private fun extractRecordKey(record: kotlinx.serialization.json.JsonObject): String? {
        return record["sync_key"]?.jsonPrimitive?.contentOrNull
            ?: record["id"]?.jsonPrimitive?.contentOrNull
    }

    override suspend fun uploadFile(
        storagePath: String,
        bytes: ByteArray,
        contentType: String
    ): Result<String> = runCatching {
        require(bytes.isNotEmpty()) { "Berkas kosong tidak dapat diunggah." }
        require(bytes.size <= 25 * 1024 * 1024) { "Ukuran berkas melebihi batas 25 MB." }

        val client = requireClient()
        val bucket = client.storage.from(SupabaseConfig.STORAGE_BUCKET)
        val parsedType = runCatching { ContentType.parse(contentType) }
            .getOrDefault(ContentType.Application.OctetStream)
        bucket.upload(path = storagePath, data = bytes) {
            upsert = true
            this.contentType = parsedType
        }
        runCatching {
            bucket.createSignedUrl(path = storagePath, expiresIn = 24.hours)
        }.getOrDefault(storagePath)
    }

    override suspend fun createDownloadUrl(storagePath: String): Result<String> = runCatching {
        val client = requireClient()
        client.storage.from(SupabaseConfig.STORAGE_BUCKET)
            .createSignedUrl(path = storagePath, expiresIn = 1.hours)
    }

    override suspend fun downloadFileBytes(storagePath: String): Result<ByteArray> = runCatching {
        val client = requireClient()
        client.storage.from(SupabaseConfig.STORAGE_BUCKET)
            .downloadAuthenticated(path = storagePath)
    }

    override suspend fun deleteFile(storagePath: String): Result<Unit> = runCatching {
        val client = requireClient()
        client.storage.from(SupabaseConfig.STORAGE_BUCKET).delete(storagePath)
        Unit
    }
}
