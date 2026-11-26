package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.models.AppRole
import com.example.data.models.AttendanceRecord
import com.example.data.models.AttendanceStatus
import com.example.data.models.ClassRoom
import com.example.data.models.DigitalArchiveItem
import com.example.data.models.HafalanRecord
import com.example.data.models.HafalanStatus
import com.example.data.models.ManagedUserProfile
import com.example.data.models.Student
import com.example.data.remote.*
import com.example.data.repository.CloudSyncEngine
import com.example.ui.AppViewModel
import com.example.ui.AuthUiState
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Simulasi backend Supabase PostgreSQL + Realtime + Storage bersama
 * yang diakses oleh dua perangkat independen (Device 1 & Device 2).
 */
class FakeSharedSupabaseCloud {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    data class AuthAccount(
        val id: String,
        val email: String,
        val password: String,
        var profile: SupabaseProfileDto
    )

    val accounts = linkedMapOf<String, AuthAccount>()
    var schoolData: SupabaseSchoolDataDto? = null
    var teacherProfile: SupabaseTeacherProfileDto? = null
    val classRooms = linkedMapOf<String, SupabaseClassRoomDto>()
    val students = linkedMapOf<String, SupabaseStudentDto>()
    val merdekaDocuments = linkedMapOf<String, SupabaseMerdekaDocumentDto>()
    val teachingJournals = linkedMapOf<String, SupabaseTeachingJournalDto>()
    val assessmentRecords = linkedMapOf<String, SupabaseAssessmentRecordDto>()
    val questions = linkedMapOf<String, SupabaseQuestionDto>()
    val religiousActivities = linkedMapOf<String, SupabaseReligiousActivityDto>()
    val extracurriculars = linkedMapOf<String, SupabaseExtracurricularDto>()
    val attendanceRecords = linkedMapOf<String, SupabaseAttendanceRecordDto>()
    val hafalanRecords = linkedMapOf<String, SupabaseHafalanRecordDto>()
    val syncMessages = linkedMapOf<String, SupabaseSyncMessageDto>()
    val digitalArchives = linkedMapOf<String, SupabaseDigitalArchiveDto>()
    val activityLogs = linkedMapOf<String, SupabaseActivityLogDto>()
    val storageFiles = linkedMapOf<String, ByteArray>()

    private val realtimeBus = MutableSharedFlow<RealtimeTableEvent>(extraBufferCapacity = 128)

    @Synchronized
    fun signUp(email: String, password: String, fullName: String, nip: String, schoolName: String): Result<ManagedUserProfile> {
        val cleanEmail = email.trim().lowercase()
        if (accounts.values.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Email already registered"))
        }
        val uid = UUID.randomUUID().toString()
        val assignedRole = if (cleanEmail == "kkgpaikecamatansukajaya@gmail.com") {
            AppRole.SUPER_ADMIN.dbValue
        } else {
            AppRole.SISWA.dbValue
        }
        val dto = SupabaseProfileDto(
            id = uid,
            email = cleanEmail,
            fullName = fullName,
            nip = nip,
            role = assignedRole,
            isActive = true,
            schoolName = schoolName.ifBlank { "SD Negeri Kiarapandak 01" }
        )
        accounts[uid] = AuthAccount(uid, cleanEmail, password, dto)
        return Result.success(dto.toManagedUser())
    }

    @Synchronized
    fun signIn(email: String, password: String): Result<ManagedUserProfile> {
        val acc = accounts.values.find { it.email.equals(email.trim(), ignoreCase = true) }
            ?: return Result.failure(IllegalStateException("Invalid login credentials"))
        if (acc.password != password) {
            return Result.failure(IllegalStateException("Invalid login credentials"))
        }
        if (!acc.profile.isActive) {
            return Result.failure(IllegalStateException("Akun dinonaktifkan oleh Admin"))
        }
        return Result.success(acc.profile.toManagedUser())
    }

    @Synchronized
    fun upsert(table: String, payloadJson: String) {
        var key = ""
        when (table) {
            SupabaseTables.SCHOOL_DATA -> {
                val dto = json.decodeFromString<SupabaseSchoolDataDto>(payloadJson)
                schoolData = dto
                key = "1"
            }
            SupabaseTables.TEACHER_PROFILES -> {
                val dto = json.decodeFromString<SupabaseTeacherProfileDto>(payloadJson)
                teacherProfile = dto
                key = "1"
            }
            SupabaseTables.CLASS_ROOMS -> {
                val dto = json.decodeFromString<SupabaseClassRoomDto>(payloadJson)
                classRooms[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.STUDENTS -> {
                val dto = json.decodeFromString<SupabaseStudentDto>(payloadJson)
                students[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.MERDEKA_DOCUMENTS -> {
                val dto = json.decodeFromString<SupabaseMerdekaDocumentDto>(payloadJson)
                merdekaDocuments[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.TEACHING_JOURNALS -> {
                val dto = json.decodeFromString<SupabaseTeachingJournalDto>(payloadJson)
                teachingJournals[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.ASSESSMENT_RECORDS -> {
                val dto = json.decodeFromString<SupabaseAssessmentRecordDto>(payloadJson)
                assessmentRecords[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.QUESTIONS -> {
                val dto = json.decodeFromString<SupabaseQuestionDto>(payloadJson)
                questions[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.RELIGIOUS_ACTIVITIES -> {
                val dto = json.decodeFromString<SupabaseReligiousActivityDto>(payloadJson)
                religiousActivities[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.EXTRACURRICULARS -> {
                val dto = json.decodeFromString<SupabaseExtracurricularDto>(payloadJson)
                extracurriculars[dto.syncKey] = dto
                key = dto.syncKey
            }
            SupabaseTables.ATTENDANCE_RECORDS -> {
                val dto = json.decodeFromString<SupabaseAttendanceRecordDto>(payloadJson)
                attendanceRecords[dto.id] = dto
                key = dto.id
            }
            SupabaseTables.HAFALAN_RECORDS -> {
                val dto = json.decodeFromString<SupabaseHafalanRecordDto>(payloadJson)
                hafalanRecords[dto.id] = dto
                key = dto.id
            }
            SupabaseTables.SYNC_MESSAGES -> {
                val dto = json.decodeFromString<SupabaseSyncMessageDto>(payloadJson)
                syncMessages[dto.id] = dto
                key = dto.id
            }
            SupabaseTables.DIGITAL_ARCHIVES -> {
                val dto = json.decodeFromString<SupabaseDigitalArchiveDto>(payloadJson)
                digitalArchives[dto.id] = dto
                key = dto.id
            }
            SupabaseTables.ACTIVITY_LOGS -> {
                val dto = json.decodeFromString<SupabaseActivityLogDto>(payloadJson)
                activityLogs[dto.syncKey] = dto
                key = dto.syncKey
            }
        }
        realtimeBus.tryEmit(RealtimeTableEvent(table, "INSERT", key, payloadJson))
    }

    @Synchronized
    fun delete(table: String, recordKey: String) {
        when (table) {
            SupabaseTables.CLASS_ROOMS -> classRooms.remove(recordKey)
            SupabaseTables.STUDENTS -> students.remove(recordKey)
            SupabaseTables.MERDEKA_DOCUMENTS -> merdekaDocuments.remove(recordKey)
            SupabaseTables.TEACHING_JOURNALS -> teachingJournals.remove(recordKey)
            SupabaseTables.ASSESSMENT_RECORDS -> assessmentRecords.remove(recordKey)
            SupabaseTables.QUESTIONS -> questions.remove(recordKey)
            SupabaseTables.RELIGIOUS_ACTIVITIES -> religiousActivities.remove(recordKey)
            SupabaseTables.EXTRACURRICULARS -> extracurriculars.remove(recordKey)
            SupabaseTables.ATTENDANCE_RECORDS -> attendanceRecords.remove(recordKey)
            SupabaseTables.HAFALAN_RECORDS -> hafalanRecords.remove(recordKey)
            SupabaseTables.SYNC_MESSAGES -> syncMessages.remove(recordKey)
            SupabaseTables.DIGITAL_ARCHIVES -> digitalArchives.remove(recordKey)
            SupabaseTables.ACTIVITY_LOGS -> activityLogs.remove(recordKey)
        }
        realtimeBus.tryEmit(RealtimeTableEvent(table, "DELETE", recordKey, null))
    }

    @Synchronized
    fun updateProfileRole(actorId: String, targetUserId: String, role: AppRole, isActive: Boolean): Result<Unit> {
        val actor = accounts[actorId]?.profile
            ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val actorRole = AppRole.fromDbValue(actor.role)
        val target = accounts[targetUserId]
            ?: return Result.failure(IllegalStateException("Target user tidak ditemukan"))
        val targetRole = AppRole.fromDbValue(target.profile.role)
        if (!actorRole.isAdminOrAbove) {
            return Result.failure(IllegalStateException("RLS violation: hanya Admin/Super Admin"))
        }
        if (actorRole == AppRole.ADMIN && (targetRole == AppRole.SUPER_ADMIN || role == AppRole.SUPER_ADMIN)) {
            return Result.failure(IllegalStateException("RLS violation: Admin tidak boleh mengubah Super Admin"))
        }
        target.profile = target.profile.copy(role = role.dbValue, isActive = isActive)
        realtimeBus.tryEmit(
            RealtimeTableEvent(SupabaseTables.PROFILES, "UPDATE", targetUserId, json.encodeToString(target.profile))
        )
        return Result.success(Unit)
    }

    @Synchronized
    fun snapshot(): RemoteDatabaseSnapshot {
        return RemoteDatabaseSnapshot(
            profiles = accounts.values.map { it.profile },
            schoolData = schoolData,
            teacherProfile = teacherProfile,
            classRooms = classRooms.values.toList(),
            students = students.values.toList(),
            merdekaDocuments = merdekaDocuments.values.toList(),
            teachingJournals = teachingJournals.values.toList(),
            assessmentRecords = assessmentRecords.values.toList(),
            questions = questions.values.toList(),
            religiousActivities = religiousActivities.values.toList(),
            extracurriculars = extracurriculars.values.toList(),
            attendanceRecords = attendanceRecords.values.toList(),
            hafalanRecords = hafalanRecords.values.toList(),
            syncMessages = syncMessages.values.toList(),
            digitalArchives = digitalArchives.values.toList(),
            activityLogs = activityLogs.values.toList()
        )
    }

    fun observeEvents(): Flow<RealtimeTableEvent> = realtimeBus.asSharedFlow()
}

class FakeDeviceGateway(
    private val cloud: FakeSharedSupabaseCloud
) : RemoteBackendGateway {

    var simulateNetworkFailure: Boolean = false
    var activeProfile: ManagedUserProfile? = null
    private var realtimeSubscribed: Boolean = false
    private val localEvents = MutableSharedFlow<RealtimeTableEvent>(extraBufferCapacity = 64)

    override val isConfigured: Boolean = true
    override val currentUserId: String?
        get() = activeProfile?.id

    override val realtimeEvents: Flow<RealtimeTableEvent> = localEvents.asSharedFlow()

    override suspend fun restoreSession(): Result<ManagedUserProfile?> = Result.success(activeProfile)

    override suspend fun signIn(email: String, password: String): Result<ManagedUserProfile> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Network timeout"))
        val res = cloud.signIn(email, password)
        res.onSuccess { activeProfile = it }
        return res
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        nip: String,
        schoolName: String
    ): Result<SignUpOutcome> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Network timeout"))
        val res = cloud.signUp(email, password, fullName, nip, schoolName)
        return res.map {
            activeProfile = it
            SignUpOutcome.Authenticated(it)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        stopRealtimeSync()
        activeProfile = null
        return Result.success(Unit)
    }

    override suspend fun fetchAllProfiles(): Result<List<ManagedUserProfile>> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Network timeout"))
        return Result.success(cloud.snapshot().profiles.map { it.toManagedUser() })
    }

    override suspend fun updateUserRoleAndStatus(userId: String, role: AppRole, isActive: Boolean): Result<Unit> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Network timeout"))
        val uid = activeProfile?.id ?: return Result.failure(IllegalStateException("Not logged in"))
        return cloud.updateProfileRole(uid, userId, role, isActive)
    }

    override suspend fun pullSnapshot(): Result<RemoteDatabaseSnapshot> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Network timeout"))
        return Result.success(cloud.snapshot())
    }

    override suspend fun upsertRecord(table: String, payloadJson: String): Result<Unit> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Koneksi internet terputus"))
        val role = activeProfile?.appRole ?: AppRole.SISWA
        if (!role.canEditAcademicData && table != SupabaseTables.SYNC_MESSAGES && table != SupabaseTables.ACTIVITY_LOGS) {
            return Result.failure(IllegalStateException("RLS: Siswa tidak memiliki hak tulis ke tabel $table"))
        }
        cloud.upsert(table, payloadJson)
        return Result.success(Unit)
    }

    override suspend fun deleteRecord(table: String, recordKey: String): Result<Unit> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Koneksi internet terputus"))
        val role = activeProfile?.appRole ?: AppRole.SISWA
        if (!role.canEditAcademicData) {
            return Result.failure(IllegalStateException("RLS: Siswa tidak memiliki hak hapus pada tabel $table"))
        }
        cloud.delete(table, recordKey)
        return Result.success(Unit)
    }

    suspend fun deliverRealtimeFromCloud(event: RealtimeTableEvent) {
        if (realtimeSubscribed && activeProfile != null) {
            localEvents.emit(event)
        }
    }

    override suspend fun startRealtimeSync() {
        realtimeSubscribed = true
    }

    override suspend fun stopRealtimeSync() {
        realtimeSubscribed = false
    }

    override suspend fun uploadFile(storagePath: String, bytes: ByteArray, contentType: String): Result<String> {
        if (simulateNetworkFailure) return Result.failure(IllegalStateException("Network timeout"))
        cloud.storageFiles[storagePath] = bytes.copyOf()
        return Result.success("https://fnztmondnvgyrkuctlty.supabase.co/storage/v1/object/sign/pai-documents/$storagePath")
    }

    override suspend fun createDownloadUrl(storagePath: String): Result<String> {
        if (!cloud.storageFiles.containsKey(storagePath)) {
            return Result.failure(IllegalStateException("File tidak ditemukan di bucket"))
        }
        return Result.success("https://fnztmondnvgyrkuctlty.supabase.co/storage/v1/object/sign/pai-documents/$storagePath")
    }

    override suspend fun downloadFileBytes(storagePath: String): Result<ByteArray> {
        val bytes = cloud.storageFiles[storagePath]
            ?: return Result.failure(IllegalStateException("File tidak ditemukan"))
        return Result.success(bytes.copyOf())
    }

    override suspend fun deleteFile(storagePath: String): Result<Unit> {
        cloud.storageFiles.remove(storagePath)
        return Result.success(Unit)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseSyncAndAuthTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var cloud: FakeSharedSupabaseCloud
    private lateinit var dbDevice1: AppDatabase
    private lateinit var dbDevice2: AppDatabase
    private lateinit var scope1: CoroutineScope
    private lateinit var scope2: CoroutineScope

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        cloud = FakeSharedSupabaseCloud()
        dbDevice1 = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dbDevice2 = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope1.cancel()
        scope2.cancel()
        dbDevice1.close()
        dbDevice2.close()
    }

    private suspend fun awaitWithMainLooper(timeoutMs: Long = 3000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            if (predicate()) return
            delay(25)
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun `auth signup assigns Super Admin to bootstrap email and Siswa to new users, plus logout works`() = runBlocking {
        val gw1 = FakeDeviceGateway(cloud)
        val viewModel = AppViewModel(app, gw1)
        viewModel.syncEngine.setNetworkConnectedForTest(true)

        // Bootstrap owner signs up -> SUPER_ADMIN
        viewModel.signUp(
            email = "kkgpaikecamatansukajaya@gmail.com",
            password = "Password123!",
            fullName = "Muhamad Suhendi, S.Pd",
            nip = "199308162024211007",
            schoolName = "SDN Kiarapandak 01"
        )
        awaitWithMainLooper { viewModel.authState.value is AuthUiState.Authenticated }
        val ownerState = viewModel.authState.value
        assertTrue(ownerState is AuthUiState.Authenticated)
        assertEquals(AppRole.SUPER_ADMIN, (ownerState as AuthUiState.Authenticated).profile.appRole)

        // Logout works
        viewModel.logout()
        awaitWithMainLooper { viewModel.authState.value is AuthUiState.Unauthenticated }
        assertTrue(viewModel.authState.value is AuthUiState.Unauthenticated)

        // Normal user signs up -> defaults to SISWA (least privilege)
        viewModel.signUp(
            email = "siswa.baru@sekolah.sch.id",
            password = "Password123!",
            fullName = "Ahmad Zaki",
            nip = "10243",
            schoolName = "SDN Kiarapandak 01"
        )
        awaitWithMainLooper { viewModel.authState.value is AuthUiState.Authenticated }
        val siswaState = viewModel.authState.value
        assertTrue(siswaState is AuthUiState.Authenticated)
        assertEquals(AppRole.SISWA, (siswaState as AuthUiState.Authenticated).profile.appRole)
    }

    @Test
    fun `RBAC blocks Siswa mutations and limits Admin from touching Super Admin`() = runBlocking {
        val superAdmin = cloud.signUp("kkgpaikecamatansukajaya@gmail.com", "Admin123!", "Ketua KKG", "1993", "SDN 01").getOrThrow()
        val adminUser = cloud.signUp("admin.kkg@sekolah.sch.id", "Admin123!", "Admin KKG", "1994", "SDN 01").getOrThrow()
        val siswaUser = cloud.signUp("siswa1@sekolah.sch.id", "Siswa123!", "Siti Aminah", "1001", "SDN 01").getOrThrow()

        cloud.updateProfileRole(superAdmin.id, adminUser.id, AppRole.ADMIN, true)

        val gw = FakeDeviceGateway(cloud)
        val viewModel = AppViewModel(app, gw)
        viewModel.syncEngine.setNetworkConnectedForTest(true)

        // Login as Siswa -> adding class is blocked
        viewModel.signIn("siswa1@sekolah.sch.id", "Siswa123!")
        awaitWithMainLooper {
            (viewModel.authState.value as? AuthUiState.Authenticated)?.profile?.id == siswaUser.id
        }
        val initialClassCount = viewModel.allClasses.value.size
        viewModel.addClass("Kelas Ilegal Siswa", "VI")
        awaitWithMainLooper(timeoutMs = 250) { false }
        assertEquals(initialClassCount, viewModel.allClasses.value.size)

        // Login as Admin -> can promote Siswa to Guru, cannot promote anyone to Super Admin
        viewModel.signIn("admin.kkg@sekolah.sch.id", "Admin123!")
        awaitWithMainLooper {
            (viewModel.authState.value as? AuthUiState.Authenticated)?.profile?.id == adminUser.id
        }
        var promoOk = false
        var promoBlocked = false
        viewModel.updateUserRoleAndStatus(siswaUser, AppRole.GURU, true) { ok, _ -> promoOk = ok }
        awaitWithMainLooper { promoOk }
        assertTrue(promoOk)
        assertEquals(AppRole.GURU.dbValue, cloud.snapshot().profiles.find { it.id == siswaUser.id }?.role)

        viewModel.updateUserRoleAndStatus(siswaUser, AppRole.SUPER_ADMIN, true) { ok, _ -> promoBlocked = !ok }
        awaitWithMainLooper { promoBlocked }
        assertTrue(promoBlocked)

        // Super Admin CAN promote Admin or Guru cleanly
        viewModel.signIn("kkgpaikecamatansukajaya@gmail.com", "Admin123!")
        awaitWithMainLooper {
            (viewModel.authState.value as? AuthUiState.Authenticated)?.profile?.id == superAdmin.id
        }
        var superOk = false
        viewModel.updateUserRoleAndStatus(siswaUser, AppRole.ADMIN, true) { ok, _ -> superOk = ok }
        awaitWithMainLooper { superOk }
        assertTrue(superOk)
        assertEquals(AppRole.ADMIN.dbValue, cloud.snapshot().profiles.find { it.id == siswaUser.id }?.role)
    }

    @Test
    fun `unstable network queues changes locally and flushes on retry`() = runBlocking {
        cloud.signUp("kkgpaikecamatansukajaya@gmail.com", "Admin123!", "Ketua KKG", "1993", "SDN 01")
        val gw1 = FakeDeviceGateway(cloud)
        assertTrue(gw1.signIn("kkgpaikecamatansukajaya@gmail.com", "Admin123!").isSuccess)

        val engine1 = CloudSyncEngine(app, dbDevice1, gw1, scope1)
        engine1.setNetworkConnectedForTest(true)

        // Simulate unstable/failing network request
        gw1.simulateNetworkFailure = true
        val cls = ClassRoom(name = "Kelas 5-Uji", level = "V")
        dbDevice1.classRoomDao().insertClass(cls)
        engine1.enqueueUpsert(SupabaseTables.CLASS_ROOMS, cls.syncKey, engine1.encodeClassRoom(cls, gw1.currentUserId))

        // Queued with retryCount incremented because upsert failed
        val queued = dbDevice1.pendingSyncDao().getAllPending()
        assertEquals(1, queued.size)
        assertEquals(1, queued.first().retryCount)
        assertTrue(queued.first().nextRetryAt > System.currentTimeMillis())
        assertTrue(cloud.classRooms.isEmpty())

        // Network recovers -> retryPendingSync succeeds and pushes to cloud!
        gw1.simulateNetworkFailure = false
        val syncResult = engine1.syncNow(forceRetry = true)
        assertTrue(syncResult.isSuccess)
        assertTrue(dbDevice1.pendingSyncDao().getAllPending().isEmpty())
        assertTrue(cloud.classRooms.containsKey(cls.syncKey))
    }

    @Test
    fun `two-device sync propagates add, edit, delete, attendance, hafalan, and storage via Realtime and reopen`() = runBlocking {
        val superAdmin = cloud.signUp("kkgpaikecamatansukajaya@gmail.com", "Admin123!", "Muhamad Suhendi", "1993", "SDN 01").getOrThrow()
        val guru = cloud.signUp("guru.pai@sekolah.sch.id", "Guru123!", "Ahmad Rapei", "1995", "SDN 02").getOrThrow()
        cloud.updateProfileRole(superAdmin.id, guru.id, AppRole.GURU, true)

        val gw1 = FakeDeviceGateway(cloud)
        val gw2 = FakeDeviceGateway(cloud)
        assertTrue(gw1.signIn("kkgpaikecamatansukajaya@gmail.com", "Admin123!").isSuccess)
        assertTrue(gw2.signIn("guru.pai@sekolah.sch.id", "Guru123!").isSuccess)

        val engine1 = CloudSyncEngine(app, dbDevice1, gw1, scope1)
        val engine2 = CloudSyncEngine(app, dbDevice2, gw2, scope2)
        engine1.setNetworkConnectedForTest(true)
        engine2.setNetworkConnectedForTest(true)
        engine1.startForAuthenticatedUser()
        engine2.startForAuthenticatedUser()

        // Pipe cloud realtime events to Device 2
        val bridgeJob = scope2.launch {
            cloud.observeEvents().collect { event ->
                gw2.deliverRealtimeFromCloud(event)
            }
        }
        delay(200)

        // 1) DEVICE 1 ADDS ClassRoom + Student + Attendance + Hafalan + Storage File
        val cls = ClassRoom(name = "Kelas 6-B", level = "VI")
        val clsId = dbDevice1.classRoomDao().insertClass(cls).toInt()
        engine1.enqueueUpsert(SupabaseTables.CLASS_ROOMS, cls.syncKey, engine1.encodeClassRoom(cls, gw1.currentUserId))

        val student = Student(
            classId = clsId,
            nis = "2026001",
            name = "Zaid Al-Khair",
            gender = "L",
            parentName = "Bpk. Khair",
            parentPhone = "0812345678",
            classSyncKey = cls.syncKey
        )
        dbDevice1.studentDao().insertStudent(student)
        engine1.enqueueUpsert(SupabaseTables.STUDENTS, student.syncKey, engine1.encodeStudent(student, gw1.currentUserId))

        val att = AttendanceRecord(
            id = "att-2026-09-22-zaid",
            date = "2026-09-22",
            classId = clsId,
            className = cls.name,
            studentId = 1,
            studentName = student.name,
            status = AttendanceStatus.HADIR
        )
        dbDevice1.attendanceDao().upsert(att)
        engine1.enqueueUpsert(SupabaseTables.ATTENDANCE_RECORDS, att.id, engine1.encodeAttendance(att, gw1.currentUserId))

        val hafalan = HafalanRecord(
            id = "haf-zaid-al-maun",
            studentId = 1,
            studentName = student.name,
            classId = clsId,
            className = cls.name,
            surahNumber = 107,
            surahName = "Al-Ma'un",
            date = "2026-09-22",
            score = 96,
            status = HafalanStatus.MUMTAZ
        )
        dbDevice1.hafalanDao().upsert(hafalan)
        engine1.enqueueUpsert(SupabaseTables.HAFALAN_RECORDS, hafalan.id, engine1.encodeHafalan(hafalan, gw1.currentUserId))

        val fileBytes = "SERTIFIKAT_PENDIDIK_PAI_2026".toByteArray()
        val uploadPath = "${gw1.currentUserId}/sertifikat.txt"
        assertTrue(gw1.uploadFile(uploadPath, fileBytes, "text/plain").isSuccess)
        val archive = DigitalArchiveItem(
            id = "arc-test-1",
            title = "Sertifikat PPG",
            category = "Sertifikat Pendidik",
            fileName = "sertifikat.txt",
            fileSize = "28 B",
            storagePath = uploadPath,
            uploadedByName = "Muhamad Suhendi"
        )
        dbDevice1.digitalArchiveDao().upsert(archive)
        engine1.enqueueUpsert(SupabaseTables.DIGITAL_ARCHIVES, archive.id, engine1.encodeDigitalArchive(archive, gw1.currentUserId))

        awaitWithMainLooper(timeoutMs = 3000L) {
            runBlocking {
                dbDevice2.classRoomDao().getClassBySyncKey(cls.syncKey) != null &&
                    dbDevice2.studentDao().getStudentBySyncKey(student.syncKey) != null &&
                    dbDevice2.digitalArchiveDao().getAllArchivesList().any { it.id == archive.id }
            }
        }

        // Verify Device 2 received ALL added items automatically via Realtime!
        assertNotNull(dbDevice2.classRoomDao().getClassBySyncKey(cls.syncKey))
        assertEquals("Zaid Al-Khair", dbDevice2.studentDao().getStudentBySyncKey(student.syncKey)?.name)
        assertEquals(1, dbDevice2.attendanceDao().getAllAttendanceList().count { it.id == att.id })
        assertEquals(96, dbDevice2.hafalanDao().getAllHafalanList().find { it.id == hafalan.id }?.score)
        assertEquals(1, dbDevice2.digitalArchiveDao().getAllArchivesList().count { it.id == archive.id })

        // Verify Device 2 can download exact Storage bytes
        val downloaded = gw2.downloadFileBytes(uploadPath).getOrThrow()
        assertArrayEquals(fileBytes, downloaded)

        // 2) DEVICE 1 EDITS Student -> Device 2 receives UPDATE via Realtime
        val editedStudent = student.copy(name = "Zaid Al-Khair (Edited)")
        dbDevice1.studentDao().insertStudent(editedStudent)
        engine1.enqueueUpsert(SupabaseTables.STUDENTS, editedStudent.syncKey, engine1.encodeStudent(editedStudent, gw1.currentUserId))
        awaitWithMainLooper(timeoutMs = 3000L) {
            runBlocking {
                dbDevice2.studentDao().getStudentBySyncKey(student.syncKey)?.name == "Zaid Al-Khair (Edited)"
            }
        }
        assertEquals("Zaid Al-Khair (Edited)", dbDevice2.studentDao().getStudentBySyncKey(student.syncKey)?.name)

        // 3) DEVICE 2 closes (stops realtime), DEVICE 1 DELETES Student, DEVICE 2 reopens (syncNow) -> Student deleted on Device 2!
        gw2.stopRealtimeSync()
        dbDevice1.studentDao().deleteBySyncKey(student.syncKey)
        engine1.enqueueDelete(SupabaseTables.STUDENTS, student.syncKey)
        delay(200)
        assertFalse(cloud.students.containsKey(student.syncKey))

        // Device 2 reopens app (syncNow pulls snapshot and reconciles delete)
        assertTrue(engine2.syncNow(forceRetry = false).isSuccess)
        assertNull(dbDevice2.studentDao().getStudentBySyncKey(student.syncKey))

        bridgeJob.cancel()
    }

    @Test
    fun `AuthScreen validates invalid email input`() {
        cloud.signUp("kkgpaikecamatansukajaya@gmail.com", "Admin123!", "Muhamad Suhendi", "1993", "SDN 01")
        val gw = FakeDeviceGateway(cloud)
        val viewModel = AppViewModel(app, gw)

        composeTestRule.setContent {
            MyApplicationTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_email_input").performTextInput("invalid-email")
        composeTestRule.onNodeWithTag("auth_password_input").performTextInput("123")
        composeTestRule.onNodeWithTag("auth_submit_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Masukkan alamat email yang valid.").assertIsDisplayed()
    }

    @Test
    fun `AdminDashboardScreen renders search and RBAC management for Super Admin`() {
        cloud.signUp("kkgpaikecamatansukajaya@gmail.com", "Admin123!", "Muhamad Suhendi", "1993", "SDN 01")
        val gw = FakeDeviceGateway(cloud)
        val viewModel = AppViewModel(app, gw)
        viewModel.syncEngine.setNetworkConnectedForTest(true)
        viewModel.signIn("kkgpaikecamatansukajaya@gmail.com", "Admin123!")
        runBlocking {
            awaitWithMainLooper { viewModel.authState.value is AuthUiState.Authenticated }
        }

        composeTestRule.setContent {
            MyApplicationTheme {
                AdminDashboardScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Dashboard Admin & Manajemen Multi-Perangkat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_search_input").assertIsDisplayed()
    }
}
