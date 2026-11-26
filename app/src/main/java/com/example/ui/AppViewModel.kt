package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.models.*
import com.example.data.repository.AppRepository
import com.example.data.repository.SyncRepository
import com.example.util.AiChatMessage
import com.example.util.GeminiAssistant
import com.example.util.ImageGenerationResult
import com.example.util.VideoGenerationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

import android.net.Uri
import android.os.Build
import com.example.data.remote.*
import com.example.data.repository.CloudSyncEngine
import com.example.data.repository.SyncBannerState

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class Unauthenticated(
        val errorMessage: String? = null,
        val infoMessage: String? = null,
        val isBusy: Boolean = false
    ) : AuthUiState
    data class Authenticated(val profile: ManagedUserProfile) : AuthUiState
}

class AppViewModel @JvmOverloads constructor(
    application: Application,
    val backendGateway: RemoteBackendGateway = SupabaseBackendGateway()
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val syncEngine = CloudSyncEngine(application, db, backendGateway, viewModelScope)
    val syncRepository = SyncRepository(application, db, syncEngine, backendGateway)
    private val repository = AppRepository(
        db.teacherProfileDao(),
        db.schoolDataDao(),
        db.classRoomDao(),
        db.studentDao(),
        db.merdekaDocumentDao(),
        db.teachingJournalDao(),
        db.assessmentRecordDao(),
        db.questionDao(),
        db.religiousActivityDao(),
        db.extracurricularDao(),
        db.activityLogDao(),
        db.attendanceDao(),
        db.hafalanDao(),
        db.digitalArchiveDao(),
        db.managedUserDao()
    )

    // --- State Expositions ---
    val teacherProfile: StateFlow<TeacherProfile> = repository.profile
        .map { it ?: TeacherProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeacherProfile())

    val schoolData: StateFlow<SchoolData> = repository.schoolData
        .map { it ?: SchoolData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolData())

    val allClasses: StateFlow<List<ClassRoom>> = repository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentCount: StateFlow<Int> = repository.studentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allDocuments: StateFlow<List<MerdekaDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJournals: StateFlow<List<TeachingJournal>> = repository.allJournals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssessments: StateFlow<List<AssessmentRecord>> = repository.allAssessments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<Question>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ReligiousActivity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExtracurriculars: StateFlow<List<Extracurricular>> = repository.allExtracurriculars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<ActivityLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val digitalArchives: StateFlow<List<DigitalArchiveItem>> = repository.allArchives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val managedUsers: StateFlow<List<ManagedUserProfile>> = repository.managedUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncBannerState> = syncEngine.syncState
    val pendingSyncCount: StateFlow<Int> = syncEngine.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val pendingOperations: StateFlow<List<PendingSyncOperation>> = syncEngine.pendingOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Auth & RBAC State ---
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    val currentUserProfile: StateFlow<ManagedUserProfile?> = _authState
        .map { (it as? AuthUiState.Authenticated)?.profile }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentAppRole: StateFlow<AppRole> = currentUserProfile
        .map { it?.appRole ?: AppRole.SISWA }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppRole.SISWA)

    private val _currentUserRole = MutableStateFlow(AppRole.SISWA.displayName)
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    val isUserLoggedIn: StateFlow<Boolean> = _authState
        .map { it is AuthUiState.Authenticated }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isOnline = MutableStateFlow(syncEngine.isNetworkConnected.value)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val isSupabaseConfigured: Boolean
        get() = backendGateway.isConfigured

    private val _actionFeedback = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val actionFeedback: SharedFlow<String> = _actionFeedback.asSharedFlow()

    // --- Assistant Generating State (Asisten AI Screen) ---
    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _aiGeneratedResponse = MutableStateFlow("")
    val aiGeneratedResponse: StateFlow<String> = _aiGeneratedResponse.asStateFlow()

    private val _lastGeneratedDocType = MutableStateFlow("Modul Ajar")
    val lastGeneratedDocType: StateFlow<String> = _lastGeneratedDocType.asStateFlow()

    private val _lastGeneratedTopic = MutableStateFlow("")
    val lastGeneratedTopic: StateFlow<String> = _lastGeneratedTopic.asStateFlow()

    // --- Dedicated Perangkat Merdeka Screen AI Generator State ---
    private val _isPerangkatAiGenerating = MutableStateFlow(false)
    val isPerangkatAiGenerating: StateFlow<Boolean> = _isPerangkatAiGenerating.asStateFlow()

    private val _perangkatAiResponse = MutableStateFlow("")
    val perangkatAiResponse: StateFlow<String> = _perangkatAiResponse.asStateFlow()

    // --- Interactive Multi-Turn AI Chat State ---
    private val _isAiChatLoading = MutableStateFlow(false)
    val isAiChatLoading: StateFlow<Boolean> = _isAiChatLoading.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                text = "Assalamu'alaikum Warahmatullahi Wabarakatuh, Bapak/Ibu Guru PAI SDN Kiarapandak 01! \uD83C\uDF1F\n\nSaya Asisten AI Guru PAI berbasis regulasi Keputusan Kepala BSKAP No. 020 Tahun 2026 (Lima Dimensi Cinta & Taksonomi SOLO).\n\nSilakan tanyakan materi PAI, konsultasi metode mengajar, atau minta saya menyusun Modul Ajar, ATP, KKTP, LKPD, hingga Soal HOTS.",
                isFromUser = false,
                modelSource = "Asisten AI PAI (CP 020/2026)"
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val prefs = application.getSharedPreferences("sim_pai_ai_prefs", android.content.Context.MODE_PRIVATE)
    private val _customGeminiApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customGeminiApiKey: StateFlow<String> = _customGeminiApiKey.asStateFlow()

    init {
        GeminiAssistant.customRuntimeApiKey = _customGeminiApiKey.value
    }

    // --- Studio Media AI (Gemini 3.1 Image & Veo 3 Video) State ---
    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _generatedImageResult = MutableStateFlow<ImageGenerationResult?>(null)
    val generatedImageResult: StateFlow<ImageGenerationResult?> = _generatedImageResult.asStateFlow()

    private val _isGeneratingVideo = MutableStateFlow(false)
    val isGeneratingVideo: StateFlow<Boolean> = _isGeneratingVideo.asStateFlow()

    private val _videoGenerationStatus = MutableStateFlow("")
    val videoGenerationStatus: StateFlow<String> = _videoGenerationStatus.asStateFlow()

    private val _generatedVideoResult = MutableStateFlow<VideoGenerationResult?>(null)
    val generatedVideoResult: StateFlow<VideoGenerationResult?> = _generatedVideoResult.asStateFlow()

    private val _mediaHistory = MutableStateFlow<List<GeneratedMediaItem>>(
        listOf(
            GeneratedMediaItem(
                type = MediaType.IMAGE,
                title = "Ilustrasi Ka'bah & Masjidil Haram",
                prompt = "Pemandangan Ka'bah dan Masjidil Haram dengan ornamen emas bercahaya saat senja",
                modelUsed = "gemini-3.1-flash-image-preview",
                aspectRatio = "16:9",
                bitmap = GeminiAssistant.createSampleEducationalBitmap("Ilustrasi Ka'bah & Masjidil Haram", "16:9", "gemini-3.1-flash-image-preview"),
                isSimulated = true
            ),
            GeneratedMediaItem(
                type = MediaType.VIDEO,
                title = "Animasi Ka'bah & Jamaah Tawaf (16:9)",
                prompt = "Cinematic aerial movement around the holy Kaaba during dawn prayer with serene light",
                modelUsed = "veo-3.1-fast-generate-preview",
                aspectRatio = "16:9",
                videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                isSimulated = true
            ),
            GeneratedMediaItem(
                type = MediaType.VIDEO,
                title = "Reels Kisah Baligh & Ibadah (9:16)",
                prompt = "Portrait orientation vertical video animation of student learning Islamic values",
                modelUsed = "veo-3.1-fast-generate-preview",
                aspectRatio = "9:16",
                videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                isSimulated = true
            )
        )
    )
    val mediaHistory: StateFlow<List<GeneratedMediaItem>> = _mediaHistory.asStateFlow()

    // --- Sukajaya PAI Administration & Teacher Monitoring ---
    private val _sukajayaTeachers = MutableStateFlow<List<SukajayaTeacherItem>>(SukajayaDataProvider.getAllTeachers())
    val sukajayaTeachers: StateFlow<List<SukajayaTeacherItem>> = _sukajayaTeachers.asStateFlow()

    private val _sukajayaSchools = MutableStateFlow<List<SukajayaSchoolProgress>>(SukajayaDataProvider.getInitialSchools())
    val sukajayaSchools: StateFlow<List<SukajayaSchoolProgress>> = _sukajayaSchools.asStateFlow()

    private val _schoolSearchQuery = MutableStateFlow("")
    val schoolSearchQuery: StateFlow<String> = _schoolSearchQuery.asStateFlow()

    private val _schoolStatusFilter = MutableStateFlow<SchoolAdminStatus?>(null)
    val schoolStatusFilter: StateFlow<SchoolAdminStatus?> = _schoolStatusFilter.asStateFlow()

    private val _selectedDesaFilter = MutableStateFlow<String?>(null)
    val selectedDesaFilter: StateFlow<String?> = _selectedDesaFilter.asStateFlow()

    private val _selectedKepegawaianFilter = MutableStateFlow<String?>(null)
    val selectedKepegawaianFilter: StateFlow<String?> = _selectedKepegawaianFilter.asStateFlow()

    val filteredSukajayaSchools: StateFlow<List<SukajayaSchoolProgress>> = combine(
        _sukajayaSchools,
        _schoolSearchQuery,
        _schoolStatusFilter,
        _selectedDesaFilter
    ) { schools, query, status, desa ->
        schools.filter { school ->
            val matchQuery = query.isBlank() ||
                school.schoolName.contains(query, ignoreCase = true) ||
                school.teacherName.contains(query, ignoreCase = true) ||
                school.desa.contains(query, ignoreCase = true) ||
                school.npsn.contains(query, ignoreCase = true) ||
                school.teachers.any { it.akunSiaga.contains(query) || it.nuptk.contains(query) }

            val matchStatus = status == null || school.status == status
            val matchDesa = desa == null || school.desa.equals(desa, ignoreCase = true)

            matchQuery && matchStatus && matchDesa
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SukajayaDataProvider.getInitialSchools())

    val filteredSukajayaTeachers: StateFlow<List<SukajayaTeacherItem>> = combine(
        _sukajayaTeachers,
        _schoolSearchQuery,
        _schoolStatusFilter,
        _selectedDesaFilter,
        _selectedKepegawaianFilter
    ) { teachers, query, status, desa, kepegawaian ->
        teachers.filter { teacher ->
            val matchQuery = query.isBlank() ||
                teacher.name.contains(query, ignoreCase = true) ||
                teacher.satminkal.contains(query, ignoreCase = true) ||
                teacher.akunSiaga.contains(query, ignoreCase = true) ||
                teacher.nuptk.contains(query, ignoreCase = true) ||
                teacher.npsn.contains(query, ignoreCase = true) ||
                teacher.desa.contains(query, ignoreCase = true)

            val matchStatus = status == null || teacher.status == status
            val matchDesa = desa == null || teacher.desa.equals(desa, ignoreCase = true)
            val matchKepegawaian = kepegawaian == null || teacher.statusKepegawaian.equals(kepegawaian, ignoreCase = true)

            matchQuery && matchStatus && matchDesa && matchKepegawaian
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SukajayaDataProvider.getAllTeachers())

    val sukajayaSummary: StateFlow<SukajayaPaiSummary> = combine(
        _sukajayaSchools,
        _sukajayaTeachers
    ) { schools, teachers ->
        val complete = schools.count { it.status == SchoolAdminStatus.LENGKAP }
        val inProg = schools.count { it.status == SchoolAdminStatus.DALAM_PROSES }
        val needHelp = schools.count { it.status == SchoolAdminStatus.PERLU_PENDAMPINGAN }
        val avg = if (teachers.isNotEmpty()) teachers.map { it.progressPercent }.average().toInt() else 92

        val pns = teachers.count { it.statusKepegawaian.startsWith("PNS") }
        val pppk = teachers.count { it.statusKepegawaian.startsWith("PPPK") }
        val pppk24 = teachers.count { it.statusKepegawaian == "PPPK 2024" }
        val pppk25 = teachers.count { it.statusKepegawaian == "PPPK 2025" }
        val pppk26 = teachers.count { it.statusKepegawaian == "PPPK 2026" }
        val honorer = teachers.count { it.statusKepegawaian == "HONORER" }
        val cert = teachers.count { it.isCertified }
        val uncert = teachers.count { !it.isCertified }

        SukajayaPaiSummary(
            totalSchools = schools.size,
            totalTeachers = teachers.size,
            pnsCount = pns,
            pppkCount = pppk,
            pppk2024Count = pppk24,
            pppk2025Count = pppk25,
            pppk2026Count = pppk26,
            honorerCount = honorer,
            certifiedCount = cert,
            uncertifiedCount = uncert,
            s1Count = 31,
            s2Count = 2,
            completeSchoolsCount = complete,
            inProgressSchoolsCount = inProg,
            needAssistanceSchoolsCount = needHelp,
            averageProgressPercent = avg,
            kkgChairperson = "Muhamad Suhendi",
            kkgSecretary = "Ahmad Rapei",
            kkgTreasurer = "Cicih Mintarsih"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SukajayaPaiSummary())

    fun setSchoolSearchQuery(query: String) {
        _schoolSearchQuery.value = query
    }

    fun setSchoolStatusFilter(status: SchoolAdminStatus?) {
        _schoolStatusFilter.value = status
    }

    fun setSelectedDesaFilter(desa: String?) {
        _selectedDesaFilter.value = desa
    }

    fun setSelectedKepegawaianFilter(kepegawaian: String?) {
        _selectedKepegawaianFilter.value = kepegawaian
    }

    fun toggleTeacherVerification(teacherNo: Int) {
        _sukajayaTeachers.value = _sukajayaTeachers.value.map { teacher ->
            if (teacher.no == teacherNo) {
                val newVerified = !teacher.verifiedByKkg
                teacher.copy(
                    verifiedByKkg = newVerified,
                    lastVerifiedDate = if (newVerified) "2026-09-22" else teacher.lastVerifiedDate,
                    notes = if (newVerified) "Telah diverifikasi Tim Validasi KKG PAI Kec. Sukajaya." else teacher.notes
                )
            } else teacher
        }
        // Sinkronisasi ke daftar Satminkal/sekolah
        syncSchoolsFromTeachers()
        logActivity("Mengubah status verifikasi guru No: $teacherNo", "Pengurus KKG PAI")
    }

    fun updateTeacherChecklistItem(
        teacherNo: Int,
        itemType: String,
        isCompleted: Boolean
    ) {
        _sukajayaTeachers.value = _sukajayaTeachers.value.map { teacher ->
            if (teacher.no == teacherNo) {
                when (itemType) {
                    "CP_ATP" -> teacher.copy(cpAtpDone = isCompleted)
                    "MODUL_AJAR" -> teacher.copy(modulAjarDone = isCompleted)
                    "PROTA_PROMES" -> teacher.copy(protaPromesDone = isCompleted)
                    "KKTP" -> teacher.copy(kktpDone = isCompleted)
                    "JURNAL" -> teacher.copy(jurnalDone = isCompleted)
                    "ASESMEN" -> teacher.copy(asesmenDone = isCompleted)
                    "EKSTRA" -> teacher.copy(ekstraDone = isCompleted)
                    else -> teacher
                }
            } else teacher
        }
        syncSchoolsFromTeachers()
    }

    private fun syncSchoolsFromTeachers() {
        val currentTeachers = _sukajayaTeachers.value
        _sukajayaSchools.value = _sukajayaSchools.value.map { school ->
            val matchedTeachers = currentTeachers.filter {
                it.satminkal.equals(school.schoolName, ignoreCase = true) ||
                (school.schoolName == "SDN CILEUKSA 5" && it.satminkal.contains("CILEUKSA 05", ignoreCase = true))
            }
            if (matchedTeachers.isNotEmpty()) {
                val allCp = matchedTeachers.all { it.cpAtpDone }
                val allModul = matchedTeachers.all { it.modulAjarDone }
                val allProta = matchedTeachers.all { it.protaPromesDone }
                val allKktp = matchedTeachers.all { it.kktpDone }
                val allJurnal = matchedTeachers.all { it.jurnalDone }
                val allAsesmen = matchedTeachers.all { it.asesmenDone }
                val allEkstra = matchedTeachers.all { it.ekstraDone }
                val allVerified = matchedTeachers.all { it.verifiedByKkg }
                school.copy(
                    teachers = matchedTeachers,
                    cpAtpDone = allCp,
                    modulAjarDone = allModul,
                    protaPromesDone = allProta,
                    kktpDone = allKktp,
                    jurnalDone = allJurnal,
                    asesmenDone = allAsesmen,
                    ekstraDone = allEkstra,
                    verifiedByKkg = allVerified
                )
            } else school
        }
    }

    fun toggleSchoolVerification(schoolId: Int) {
        _sukajayaSchools.value = _sukajayaSchools.value.map { school ->
            if (school.id == schoolId) {
                val newVerified = !school.verifiedByKkg
                school.copy(
                    verifiedByKkg = newVerified,
                    lastVerifiedDate = if (newVerified) "2026-09-22" else school.lastVerifiedDate,
                    notes = if (newVerified) "Telah diverifikasi langsung oleh Tim Validasi KKG PAI Kec. Sukajaya." else school.notes
                )
            } else school
        }
        logActivity("Mengubah status verifikasi administrasi sekolah ID: $schoolId", "Pengurus KKG PAI")
    }

    fun updateSchoolChecklistItem(
        schoolId: Int,
        itemType: String,
        isCompleted: Boolean
    ) {
        _sukajayaSchools.value = _sukajayaSchools.value.map { school ->
            if (school.id == schoolId) {
                when (itemType) {
                    "CP_ATP" -> school.copy(cpAtpDone = isCompleted)
                    "MODUL_AJAR" -> school.copy(modulAjarDone = isCompleted)
                    "PROTA_PROMES" -> school.copy(protaPromesDone = isCompleted)
                    "KKTP" -> school.copy(kktpDone = isCompleted)
                    "JURNAL" -> school.copy(jurnalDone = isCompleted)
                    "ASESMEN" -> school.copy(asesmenDone = isCompleted)
                    "EKSTRA" -> school.copy(ekstraDone = isCompleted)
                    else -> school
                }
            } else school
        }
    }

    // --- Search Query Students ---
    private val _studentSearchQuery = MutableStateFlow("")
    val studentSearchQuery: StateFlow<String> = _studentSearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchedStudents: StateFlow<List<Student>> = _studentSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allStudents
            else repository.searchStudents(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            syncEngine.isNetworkConnected.collect { connected ->
                _isOnline.value = connected
                syncRepository.updateOnlineStatus(connected)
            }
        }
        viewModelScope.launch {
            try {
                val current = repository.allStudents.first()
                if (current.size < 50) {
                    loadFullDapodikStudents(force = false)
                }
                syncCp020ReferenceDocuments()
                seedInitialAttendanceAndHafalanIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            restoreSupabaseSession()
        }
    }

    private suspend fun seedInitialAttendanceAndHafalanIfEmpty() {
        if (repository.getAllAttendanceList().isEmpty()) {
            repository.upsertAttendanceAll(createInitialAttendanceRecords())
        }
        if (repository.getAllHafalanList().isEmpty()) {
            repository.upsertHafalanAll(createInitialHafalanRecords())
        }
    }

    private suspend fun restoreSupabaseSession() {
        if (!backendGateway.isConfigured) {
            _authState.value = AuthUiState.Unauthenticated(
                infoMessage = "Konfigurasikan SUPABASE_PUBLISHABLE_KEY pada Secrets panel AI Studio untuk mengaktifkan login & sinkronisasi cloud."
            )
            return
        }
        val restored = backendGateway.restoreSession()
        val profile = restored.getOrNull()
        if (profile != null && profile.isActive) {
            onUserAuthenticated(profile, isInitialLogin = false)
        } else {
            _authState.value = AuthUiState.Unauthenticated()
        }
    }

    private suspend fun onUserAuthenticated(profile: ManagedUserProfile, isInitialLogin: Boolean) {
        repository.upsertManagedUser(profile)
        _currentUserRole.value = profile.appRole.displayName
        _authState.value = AuthUiState.Authenticated(profile)
        syncRepository.bindAuthenticatedProfile(profile)
        syncEngine.startForAuthenticatedUser()
        if (profile.appRole.canEditAcademicData) {
            viewModelScope.launch {
                syncEngine.pushLocalBootstrapIfRemoteEmpty()
            }
        }
        if (isInitialLogin) {
            logActivity("Login berhasil sebagai ${profile.appRole.displayName} (${profile.email})", profile.appRole.displayName)
        }
    }

    fun signIn(email: String, password: String) {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _authState.value = AuthUiState.Unauthenticated(errorMessage = "Masukkan alamat email yang valid.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthUiState.Unauthenticated(errorMessage = "Kata sandi minimal 6 karakter.")
            return
        }
        _authState.value = AuthUiState.Unauthenticated(isBusy = true)
        viewModelScope.launch {
            val res = backendGateway.signIn(cleanEmail, password)
            if (res.isSuccess) {
                onUserAuthenticated(res.getOrThrow(), isInitialLogin = true)
            } else {
                _authState.value = AuthUiState.Unauthenticated(
                    errorMessage = mapFriendlyAuthError(res.exceptionOrNull())
                )
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, nip: String, schoolName: String) {
        val cleanEmail = email.trim()
        val cleanName = fullName.trim()
        if (cleanName.length < 3) {
            _authState.value = AuthUiState.Unauthenticated(errorMessage = "Nama lengkap minimal 3 karakter.")
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _authState.value = AuthUiState.Unauthenticated(errorMessage = "Format email tidak valid.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthUiState.Unauthenticated(errorMessage = "Kata sandi minimal 6 karakter.")
            return
        }
        _authState.value = AuthUiState.Unauthenticated(isBusy = true)
        viewModelScope.launch {
            val res = backendGateway.signUp(cleanEmail, password, cleanName, nip.trim(), schoolName.trim())
            if (res.isSuccess) {
                when (val outcome = res.getOrThrow()) {
                    is SignUpOutcome.Authenticated -> {
                        onUserAuthenticated(outcome.profile, isInitialLogin = true)
                    }
                    is SignUpOutcome.EmailConfirmationRequired -> {
                        _authState.value = AuthUiState.Unauthenticated(
                            infoMessage = "Pendaftaran berhasil untuk ${outcome.email}. Silakan cek email verifikasi (atau konfirmasi di Supabase Auth) lalu login."
                        )
                    }
                }
            } else {
                _authState.value = AuthUiState.Unauthenticated(
                    errorMessage = mapFriendlyAuthError(res.exceptionOrNull())
                )
            }
        }
    }

    fun clearAuthMessages() {
        val current = _authState.value
        if (current is AuthUiState.Unauthenticated) {
            _authState.value = current.copy(errorMessage = null, infoMessage = null)
        }
    }

    private fun mapFriendlyAuthError(error: Throwable?): String {
        val raw = error?.localizedMessage.orEmpty()
        return when {
            raw.contains("Invalid login credentials", ignoreCase = true) -> "Email atau kata sandi salah."
            raw.contains("Email not confirmed", ignoreCase = true) -> "Email belum diverifikasi. Periksa kotak masuk atau aktifkan akun di Supabase."
            raw.contains("already registered", ignoreCase = true) -> "Email ini sudah terdaftar. Silakan masuk."
            raw.contains("SUPABASE_PUBLISHABLE_KEY", ignoreCase = true) -> raw
            raw.contains("Unable to resolve host", ignoreCase = true) || raw.contains("timeout", ignoreCase = true) ->
                "Tidak dapat terhubung ke server Supabase. Periksa koneksi internet Anda."
            raw.isNotBlank() -> raw
            else -> "Gagal menghubungi server autentikasi Supabase."
        }
    }

    fun onAppResumed() {
        if (_authState.value is AuthUiState.Authenticated) {
            viewModelScope.launch {
                syncEngine.syncNow(forceRetry = false)
            }
        }
    }

    fun retryPendingSync(onResult: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val res = syncEngine.syncNow(forceRetry = true)
            val msg = if (res.isSuccess) {
                "Sinkronisasi cloud & antrean offline berhasil."
            } else {
                "Koneksi tidak stabil: ${res.exceptionOrNull()?.localizedMessage ?: "tersimpan di antrean"}"
            }
            onResult?.invoke(msg)
        }
    }

    fun refreshUsers() {
        if (!backendGateway.isConfigured) return
        viewModelScope.launch {
            backendGateway.fetchAllProfiles().onSuccess { list ->
                repository.upsertManagedUsers(list)
            }
        }
    }

    fun updateUserRoleAndStatus(
        targetUser: ManagedUserProfile,
        newRole: AppRole,
        isActive: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val actor = currentUserProfile.value
        val actorRole = actor?.appRole ?: AppRole.SISWA
        if (!actorRole.isAdminOrAbove) {
            onResult(false, "Akses ditolak: hanya Super Admin atau Admin yang dapat mengelola pengguna.")
            return
        }
        if (actorRole == AppRole.ADMIN && (targetUser.appRole == AppRole.SUPER_ADMIN || newRole == AppRole.SUPER_ADMIN)) {
            onResult(false, "Hanya Super Admin yang dapat mengelola hak akses Super Admin.")
            return
        }
        if (actor?.id == targetUser.id && (!isActive || newRole != actorRole)) {
            onResult(false, "Anda tidak dapat menonaktifkan atau menurunkan peran akun sendiri.")
            return
        }
        viewModelScope.launch {
            val res = backendGateway.updateUserRoleAndStatus(targetUser.id, newRole, isActive)
            if (res.isSuccess) {
                repository.upsertManagedUser(targetUser.copy(role = newRole.dbValue, isActive = isActive))
                logActivity("Mengubah hak akses ${targetUser.fullName} menjadi ${newRole.displayName} (${if (isActive) "Aktif" else "Nonaktif"})")
                onResult(true, "Hak akses ${targetUser.fullName} diperbarui ke ${newRole.displayName}.")
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Gagal memperbarui hak akses.")
            }
        }
    }

    private fun requireStaffPermission(actionLabel: String): Boolean {
        val role = currentAppRole.value
        // If not yet logged in during unit test seeding, allow local init; if logged in as SISWA, block mutations.
        if (_authState.value is AuthUiState.Authenticated && !role.canEditAcademicData) {
            _actionFeedback.tryEmit("Mode Siswa hanya baca (read-only). Tidak dapat $actionLabel.")
            return false
        }
        return true
    }

    // --- Actions ---

    fun changeUserRole(role: String) {
        // Role display preference only when not authenticated; when authenticated, role comes from Supabase RBAC.
        if (_authState.value !is AuthUiState.Authenticated) {
            _currentUserRole.value = role
        }
    }

    fun login(role: String) {
        _currentUserRole.value = role
    }

    fun logout() {
        val activeProfile = currentUserProfile.value
        viewModelScope.launch {
            if (activeProfile != null) {
                logActivity("Pengguna keluar dari sistem (${activeProfile.email})", activeProfile.appRole.displayName)
            }
            syncEngine.stopOnSignOut()
            backendGateway.signOut()
            syncRepository.bindAuthenticatedProfile(null)
            _currentUserRole.value = AppRole.SISWA.displayName
            _authState.value = AuthUiState.Unauthenticated(infoMessage = "Anda telah keluar dari sesi akun.")
        }
    }

    fun toggleOnlineStatus() {
        val next = !_isOnline.value
        _isOnline.value = next
        syncRepository.updateOnlineStatus(next)
        logActivity("Status koneksi: ${if (next) "Online (Supabase)" else "Mode Offline (Antrean Lokal)"}")
    }

    fun logActivity(text: String, userType: String = _currentUserRole.value) {
        val profile = currentUserProfile.value
        val log = ActivityLog(
            text = text,
            userType = profile?.appRole?.displayName ?: userType,
            userEmail = profile?.email ?: "",
            userName = profile?.fullName ?: teacherProfile.value.fullName,
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        )
        viewModelScope.launch {
            repository.insertLog(log)
            if (backendGateway.currentUserId != null) {
                syncEngine.enqueueUpsert(
                    SupabaseTables.ACTIVITY_LOGS,
                    log.syncKey,
                    syncEngine.encodeActivityLog(log, backendGateway.currentUserId)
                )
            }
        }
    }

    // --- Teacher & School Updates ---
    fun updateTeacherProfile(profile: TeacherProfile) {
        if (!requireStaffPermission("mengubah profil guru")) return
        if (profile.fullName.isBlank()) return
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.insertOrUpdateProfile(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.TEACHER_PROFILES,
                "1",
                syncEngine.encodeTeacherProfile(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui Profil Administrasi Guru: ${updated.fullName}")
        }
    }

    fun resetTeacherProfile() {
        if (!requireStaffPermission("mereset profil guru")) return
        viewModelScope.launch {
            val defaultProfile = TeacherProfile()
            repository.clearProfile()
            repository.insertOrUpdateProfile(defaultProfile)
            syncEngine.enqueueUpsert(
                SupabaseTables.TEACHER_PROFILES,
                "1",
                syncEngine.encodeTeacherProfile(defaultProfile, backendGateway.currentUserId)
            )
            logActivity("Mereset Profil Guru ke default sistem")
        }
    }

    fun updateSchoolData(data: SchoolData) {
        if (!requireStaffPermission("mengubah data sekolah")) return
        if (data.schoolName.isBlank()) return
        val updated = data.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.insertOrUpdateSchoolData(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.SCHOOL_DATA,
                "1",
                syncEngine.encodeSchoolData(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui Data Sekolah: ${updated.schoolName}")
        }
    }

    // --- Classroom Actions ---
    fun addClass(name: String, level: String) {
        if (!requireStaffPermission("menambah kelas")) return
        val cleanName = name.trim()
        val cleanLevel = level.trim()
        if (cleanName.isBlank() || cleanLevel.isBlank()) return
        viewModelScope.launch {
            val entity = ClassRoom(name = cleanName, level = cleanLevel)
            val id = repository.insertClass(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.CLASS_ROOMS,
                entity.syncKey,
                syncEngine.encodeClassRoom(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah kelas baru: $cleanName (ID: $id)")
        }
    }

    fun updateClass(classRoom: ClassRoom) {
        if (!requireStaffPermission("mengubah kelas")) return
        if (classRoom.name.isBlank()) return
        val updated = classRoom.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateClass(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.CLASS_ROOMS,
                updated.syncKey,
                syncEngine.encodeClassRoom(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui kelas: ${updated.name}")
        }
    }

    fun deleteClass(classRoom: ClassRoom) {
        if (!requireStaffPermission("menghapus kelas")) return
        viewModelScope.launch {
            repository.deleteClass(classRoom)
            syncEngine.enqueueDelete(SupabaseTables.CLASS_ROOMS, classRoom.syncKey)
            logActivity("Menghapus kelas: ${classRoom.name}")
        }
    }

    // --- Student Actions ---
    fun addStudent(classId: Int, nis: String, name: String, gender: String, parentName: String, parentPhone: String) {
        if (!requireStaffPermission("menambah siswa")) return
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val classSyncKey = repository.getClassById(classId)?.syncKey.orEmpty()
            val entity = Student(
                classId = classId,
                nis = nis.trim(),
                name = cleanName,
                gender = if (gender.uppercase() == "P") "P" else "L",
                parentName = parentName.trim(),
                parentPhone = parentPhone.trim(),
                classSyncKey = classSyncKey
            )
            repository.insertStudent(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.STUDENTS,
                entity.syncKey,
                syncEngine.encodeStudent(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah siswa baru: $cleanName (NIS: ${entity.nis})")
        }
    }

    fun updateStudent(student: Student) {
        if (!requireStaffPermission("mengubah data siswa")) return
        if (student.name.isBlank()) return
        viewModelScope.launch {
            val classSyncKey = student.classSyncKey.ifBlank { repository.getClassById(student.classId)?.syncKey.orEmpty() }
            val updated = student.copy(classSyncKey = classSyncKey, updatedAt = System.currentTimeMillis())
            repository.updateStudent(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.STUDENTS,
                updated.syncKey,
                syncEngine.encodeStudent(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui data siswa: ${updated.name}")
        }
    }

    fun deleteStudent(student: Student) {
        if (!requireStaffPermission("menghapus siswa")) return
        viewModelScope.launch {
            repository.deleteStudent(student)
            syncEngine.enqueueDelete(SupabaseTables.STUDENTS, student.syncKey)
            logActivity("Menghapus siswa: ${student.name}")
        }
    }

    fun clearStudentsInClass(classId: Int, className: String) {
        if (!requireStaffPermission("menghapus siswa di kelas")) return
        viewModelScope.launch {
            val toDelete = allStudents.value.filter { it.classId == classId }
            repository.deleteStudentsByClass(classId)
            toDelete.forEach { syncEngine.enqueueDelete(SupabaseTables.STUDENTS, it.syncKey) }
            logActivity("Membersihkan semua data siswa di kelas: $className")
        }
    }

    fun clearAllStudents() {
        if (!requireStaffPermission("menghapus semua siswa")) return
        viewModelScope.launch {
            val toDelete = allStudents.value
            repository.deleteAllStudents()
            toDelete.forEach { syncEngine.enqueueDelete(SupabaseTables.STUDENTS, it.syncKey) }
            logActivity("Membersihkan seluruh data siswa dari semua kelas")
        }
    }

    fun setStudentSearchQuery(query: String) {
        _studentSearchQuery.value = query
    }

    // --- Merdeka Documents Actions ---
    fun addDocument(
        type: String,
        title: String,
        classLevel: String,
        contentText: String,
        subject: String = "Pendidikan Agama Islam dan Budi Pekerti",
        fileUri: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        fileMimeType: String? = null,
        semester: String = "Ganjil",
        academicYear: String = "2026/2027",
        authorTeacher: String = teacherProfile.value.fullName
    ) {
        if (!requireStaffPermission("menambah dokumen pembelajaran")) return
        if (title.isBlank()) return
        viewModelScope.launch {
            val doc = MerdekaDocument(
                type = type,
                title = title.trim(),
                classLevel = classLevel,
                contentText = contentText,
                subject = subject,
                fileUri = fileUri,
                fileName = fileName,
                fileSize = fileSize,
                fileMimeType = fileMimeType,
                semester = semester,
                academicYear = academicYear,
                authorTeacher = authorTeacher
            )
            repository.insertDocument(doc)
            syncEngine.enqueueUpsert(
                SupabaseTables.MERDEKA_DOCUMENTS,
                doc.syncKey,
                syncEngine.encodeDocument(doc, backendGateway.currentUserId)
            )
            logActivity("Menambah dokumen pembelajaran baru: ${doc.title} ($type)")
        }
    }

    fun updateDocument(document: MerdekaDocument) {
        if (!requireStaffPermission("mengubah dokumen pembelajaran")) return
        val updated = document.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateDocument(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.MERDEKA_DOCUMENTS,
                updated.syncKey,
                syncEngine.encodeDocument(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui dokumen pembelajaran: ${updated.title} (${updated.type})")
        }
    }

    fun deleteDocument(document: MerdekaDocument) {
        if (!requireStaffPermission("menghapus dokumen")) return
        viewModelScope.launch {
            repository.deleteDocument(document)
            syncEngine.enqueueDelete(SupabaseTables.MERDEKA_DOCUMENTS, document.syncKey)
            logActivity("Menghapus dokumen: ${document.title}")
        }
    }

    fun deleteDocumentById(id: Int) {
        if (!requireStaffPermission("menghapus dokumen")) return
        viewModelScope.launch {
            val doc = allDocuments.value.find { it.id == id }
            repository.deleteDocumentById(id)
            if (doc != null) {
                syncEngine.enqueueDelete(SupabaseTables.MERDEKA_DOCUMENTS, doc.syncKey)
            }
            logActivity("Menghapus dokumen pembelajaran ID: $id")
        }
    }

    fun clearDocumentsByType(type: String) {
        if (!requireStaffPermission("menghapus dokumen kategori $type")) return
        viewModelScope.launch {
            val toDelete = allDocuments.value.filter { it.type == type }
            repository.deleteDocumentsByType(type)
            toDelete.forEach { syncEngine.enqueueDelete(SupabaseTables.MERDEKA_DOCUMENTS, it.syncKey) }
            logActivity("Membersihkan seluruh dokumen kategori: $type")
        }
    }

    fun clearAllDocuments() {
        if (!requireStaffPermission("menghapus seluruh dokumen")) return
        viewModelScope.launch {
            val toDelete = allDocuments.value
            repository.deleteAllDocuments()
            toDelete.forEach { syncEngine.enqueueDelete(SupabaseTables.MERDEKA_DOCUMENTS, it.syncKey) }
            logActivity("Membersihkan seluruh dokumen perangkat pembelajaran")
        }
    }

    // --- AI Generator Assistant ---
    fun updateCustomGeminiApiKey(newKey: String) {
        val cleaned = newKey.trim()
        _customGeminiApiKey.value = cleaned
        GeminiAssistant.customRuntimeApiKey = cleaned
        prefs.edit().putString("custom_gemini_api_key", cleaned).apply()
        logActivity(if (cleaned.isNotEmpty()) "Memperbarui konfigurasi Kunci API Gemini AI" else "Mengatur ulang Kunci API ke mode pintar bawaan")
    }

    fun sendAiChatMessage(userMessage: String, classLevel: String = "IV") {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty() || _isAiChatLoading.value) return

        val detectedType = GeminiAssistant.detectDocTypeFromPrompt(trimmed)
        val userBubble = AiChatMessage(
            text = trimmed,
            isFromUser = true,
            docTypeHint = detectedType,
            classLevelHint = classLevel,
            modelSource = "Guru PAI"
        )
        val currentHistory = _aiChatMessages.value
        _aiChatMessages.value = currentHistory + userBubble
        _isAiChatLoading.value = true

        viewModelScope.launch {
            val reply = GeminiAssistant.chatWithPAI(
                history = currentHistory,
                userMessage = trimmed,
                classLevel = classLevel
            )
            val aiBubble = AiChatMessage(
                text = reply,
                isFromUser = false,
                docTypeHint = if (detectedType == "Konsultasi PAI") "Bahan Ajar" else detectedType,
                classLevelHint = classLevel,
                modelSource = if (GeminiAssistant.hasValidApiKey()) "Gemini 3.5 Flash (Online)" else "Mesin Pintar PAI (CP 020/2026)"
            )
            _aiChatMessages.value = _aiChatMessages.value + aiBubble
            _isAiChatLoading.value = false
            logActivity("Konsultasi Asisten AI PAI (Kelas $classLevel): ${trimmed.take(40)}")
        }
    }

    fun clearAiChatHistory() {
        _aiChatMessages.value = listOf(
            AiChatMessage(
                text = "Riwayat percakapan telah dibersihkan. Silakan ajukan pertanyaan atau instruksi penyusunan perangkat pembelajaran PAI baru.",
                isFromUser = false,
                modelSource = "Asisten AI PAI (CP 020/2026)"
            )
        )
    }

    fun generateDocumentWithAI(docType: String, topic: String, classLevel: String) {
        val resolvedType = if (docType == "Dokumen" || docType.isBlank()) {
            val detected = GeminiAssistant.detectDocTypeFromPrompt(topic)
            if (detected == "Konsultasi PAI") "Modul Ajar" else detected
        } else {
            docType
        }
        val cleanTopic = GeminiAssistant.extractCleanTopic(topic)

        _isAiGenerating.value = true
        _aiGeneratedResponse.value = ""
        _lastGeneratedDocType.value = resolvedType
        _lastGeneratedTopic.value = cleanTopic
        
        viewModelScope.launch {
            val prompt = """
                Buatkan dokumen lengkap '$resolvedType' untuk mata pelajaran Pendidikan Agama Islam dan Budi Pekerti (PAI-BP) Sekolah Dasar Kurikulum Merdeka Kelas $classLevel dengan tema/materi pokok: '$cleanTopic'.
                
                Pedoman dan Referensi Wajib:
                1. Gunakan Keputusan Kepala BSKAP Nomor 020 Tahun 2026 tentang Perubahan atas Keputusan Nomor 046/H/KR/2025 tentang Capaian Pembelajaran (CP) PAUD, Dikdas, dan Dikmen sebagai referensi mutlak.
                2. Integrasikan 'Lima Dimensi Cinta': Cinta Allah SWT & Rasul-Nya, Cinta Al-Qur'an & Hadis, Cinta Diri & Sesama Manusia, Cinta Tanah Air & Bangsa, serta Cinta Lingkungan Hidup.
                3. Terapkan Taksonomi SOLO (Structure of Observed Learning Outcomes) dalam merumuskan indikator ketercapaian dan instrumen asesmen pembelajaran.
                4. Cakup elemen PAI yang relevan (Al-Qur'an Hadis, Akidah, Akhlak, Fikih, atau Sejarah Peradaban Islam) dengan muatan moderasi beragama yang aplikatif.
                5. Tuliskan dokumen secara lengkap, profesional, sistematis, dan siap digunakan oleh guru SDN Kiarapandak 01.
            """.trimIndent()

            val systemPrompt = """
                Anda adalah Asisten Pakar Perangkat Pembelajaran Guru PAI & Budi Pekerti Sekolah Dasar berbasis Kurikulum Merdeka.
                Seluruh penyusunan dokumen Anda berpedoman penuh pada Keputusan Kepala BSKAP Nomor 020 Tahun 2026 sebagai standar regulasi capaian pembelajaran terbaru.
                Format penulisan harus baku, rapi, bernas, dan langsung dapat dicetak atau diarsipkan oleh guru.
            """.trimIndent()
            
            val result = GeminiAssistant.generatePAIContent(
                prompt = prompt,
                systemPrompt = systemPrompt,
                docType = resolvedType,
                topic = cleanTopic,
                classLevel = classLevel
            )
            _aiGeneratedResponse.value = result
            _isAiGenerating.value = false
            logActivity("Menyusun perangkat $resolvedType - $cleanTopic (Kelas $classLevel) berpedoman pada CP 020/2026")
        }
    }

    fun generatePerangkatWithAI(docType: String, topic: String, classLevel: String) {
        val cleanTopic = GeminiAssistant.extractCleanTopic(topic)
        _isPerangkatAiGenerating.value = true
        _perangkatAiResponse.value = ""

        viewModelScope.launch {
            val prompt = "Buatkan dokumen lengkap '$docType' PAI SD Kurikulum Merdeka (SK BSKAP No. 020 Tahun 2026) Kelas $classLevel tentang '$cleanTopic'."
            val result = GeminiAssistant.generatePAIContent(
                prompt = prompt,
                docType = docType,
                topic = cleanTopic,
                classLevel = classLevel
            )
            _perangkatAiResponse.value = result
            _isPerangkatAiGenerating.value = false
            logActivity("Menyusun otomatis $docType - $cleanTopic (Kelas $classLevel) di menu Perangkat")
        }
    }

    fun clearPerangkatAiResponse() {
        _perangkatAiResponse.value = ""
    }

    fun saveGeneratedQuestionsToBank(rawText: String, topic: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val parsedList = GeminiAssistant.parseQuestionsFromText(rawText, topic.ifBlank { "PAI CP 020" })
            for (q in parsedList) {
                repository.insertQuestion(
                    Question(
                        text = q.text,
                        optionA = q.optionA,
                        optionB = q.optionB,
                        optionC = q.optionC,
                        optionD = q.optionD,
                        answerKey = q.answerKey,
                        difficulty = q.difficulty,
                        type = "Pilihan Ganda",
                        explanation = q.explanation
                    )
                )
            }
            if (parsedList.isNotEmpty()) {
                logActivity("Menyimpan ${parsedList.size} soal HOTS hasil Asisten AI ke Bank Soal PAI")
            }
            onComplete(parsedList.size)
        }
    }

    fun triggerInstantCloudSync(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = syncEngine.syncNow(forceRetry = true)
            val docsCount = allDocuments.value.size
            val studentsCount = allStudents.value.size
            val journalsCount = allJournals.value.size
            if (result.isSuccess) {
                logActivity("Sinkronisasi Supabase berhasil: $studentsCount siswa, $docsCount perangkat, $journalsCount jurnal")
                onComplete?.invoke("Sinkronisasi Supabase Berhasil ($studentsCount Siswa, $docsCount Dokumen, $journalsCount Jurnal)")
            } else {
                val pending = pendingSyncCount.value
                onComplete?.invoke("Offline/tidak stabil: $pending perubahan tersimpan di antrean ulang otomatis.")
            }
        }
    }

    // --- Studio Media AI Actions (Gemini 3.1 & Veo 3) ---
    fun generateImage(
        prompt: String,
        inputBitmap: Bitmap? = null,
        aspectRatio: String = "1:1",
        imageSize: String = "1K"
    ) {
        _isGeneratingImage.value = true
        viewModelScope.launch {
            val base64 = inputBitmap?.let { bitmapToBase64(it) }
            val result = GeminiAssistant.generateOrEditImage(
                prompt = prompt,
                inputImageBase64 = base64,
                aspectRatio = aspectRatio,
                imageSize = imageSize
            )
            _generatedImageResult.value = result
            _isGeneratingImage.value = false

            if (result.bitmap != null) {
                val mediaItem = GeneratedMediaItem(
                    type = MediaType.IMAGE,
                    title = prompt.take(45),
                    prompt = prompt,
                    modelUsed = result.modelUsed,
                    aspectRatio = aspectRatio,
                    bitmap = result.bitmap,
                    sourceImageBitmap = inputBitmap,
                    isSimulated = result.isSimulated
                )
                _mediaHistory.value = listOf(mediaItem) + _mediaHistory.value
                logActivity("Membuat gambar AI (${result.modelUsed}): $prompt")
            }
        }
    }

    fun generateVideo(
        prompt: String,
        inputBitmap: Bitmap? = null,
        aspectRatio: String = "16:9"
    ) {
        _isGeneratingVideo.value = true
        _videoGenerationStatus.value = "Menyiapkan parameter Veo 3..."
        viewModelScope.launch {
            val base64 = inputBitmap?.let { bitmapToBase64(it) }
            val result = GeminiAssistant.generateVideoWithVeo(
                prompt = prompt,
                inputImageBase64 = base64,
                aspectRatio = aspectRatio,
                onStatusUpdate = { status ->
                    _videoGenerationStatus.value = status
                }
            )
            _generatedVideoResult.value = result
            _isGeneratingVideo.value = false

            if (result.videoUri != null) {
                val mediaItem = GeneratedMediaItem(
                    type = MediaType.VIDEO,
                    title = prompt.take(45),
                    prompt = prompt,
                    modelUsed = result.modelUsed,
                    aspectRatio = result.aspectRatio,
                    videoUri = result.videoUri,
                    sourceImageBitmap = inputBitmap,
                    isSimulated = result.isSimulated
                )
                _mediaHistory.value = listOf(mediaItem) + _mediaHistory.value
                logActivity("Membuat video AI (${result.modelUsed}, ${result.aspectRatio}): $prompt")
            }
        }
    }

    fun deleteMediaItem(id: String) {
        _mediaHistory.value = _mediaHistory.value.filterNot { it.id == id }
    }

    fun clearCurrentMediaResults() {
        _generatedImageResult.value = null
        _generatedVideoResult.value = null
        _videoGenerationStatus.value = ""
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun syncCp020ReferenceDocuments() {
        try {
            val existingDocs = repository.allDocuments.first()
            val hasCp020 = existingDocs.any { it.contentText.contains("020 Tahun 2026") || it.title.contains("020/2026") }
            if (!hasCp020) {
                repository.insertDocument(
                    MerdekaDocument(
                        type = "CP",
                        title = "Capaian Pembelajaran PAI SD (SK BSKAP 020/2026)",
                        classLevel = "Semua",
                        contentText = """
                            CAPAIAN PEMBELAJARAN PENDIDIKAN AGAMA ISLAM DAN BUDI PEKERTI (PAI-BP)
                            Berdasarkan Keputusan Kepala BSKAP Nomor 020 Tahun 2026
                            (Perubahan atas Keputusan Nomor 046/H/KR/2025)
                            
                            I. RASIONALITAS & LIMA DIMENSI CINTA
                            Pendidikan Agama Islam dan Budi Pekerti berorientasi pada pembentukan karakter beriman, bertakwa, dan berakhlak mulia melalui penanaman 'Lima Dimensi Cinta':
                            1. Cinta Allah SWT dan Rasul-Nya
                            2. Cinta Al-Qur'an dan Hadis
                            3. Cinta Diri Sendiri dan Sesama Manusia
                            4. Cinta Tanah Air dan Bangsa
                            5. Cinta Lingkungan Hidup dan Alam Sekitar
                            
                            II. CAPAIAN PEMBELAJARAN PER FASE
                            1. Fase A (Kelas I dan II SD):
                               - Al-Qur'an Hadis: Mengenal huruf hijaiyah berharakat, membaca surah Al-Fatihah dan surah pendek pilihan dengan bimbingan.
                               - Akidah: Mengenal rukun iman dan rukun Islam serta asmaulhusna secara sederhana.
                               - Akhlak: Membiasakan sikap jujur, santun, dan menyayangi keluarga serta teman.
                               - Fikih: Mengenal tata cara bersuci (wudu) dan gerakan salat fardhu.
                               - SPI: Mengenal kisah ringkas kelahiran dan masa kanak-kanak Nabi Muhammad SAW.
                            
                            2. Fase B (Kelas III dan IV SD):
                               - Al-Qur'an Hadis: Membaca, menghafal, dan memahami pesan pokok surah-surah pendek (Q.S. Al-Hujurat:13) dengan tartil.
                               - Akidah: Mengimani rasul-rasul Allah dan meneladani asmaulhusna (Al-Malik, Al-Aziz, Al-Quddus, As-Salam, Al-Mu'min).
                               - Akhlak: Menumbuhkan sikap toleran (tasamuh), menghargai keragaman suku bangsa, serta peduli lingkungan.
                               - Fikih: Memahami tanda-tanda usia baligh, taharah (mandi wajib), dan salat berjamaah.
                               - SPI: Menceritakan peristiwa hijrah Nabi Muhammad SAW ke Madinah dan membangun kerukunan.
                            
                            3. Fase C (Kelas V dan VI SD):
                               - Al-Qur'an Hadis: Menganalisis pesan pokok Q.S. Al-Ma'un dan Q.S. Ali Imran:64 serta hadis tentang persaudaraan.
                               - Akidah: Mengimani hari akhir dan qada/qadar serta dampaknya terhadap keteguhan hati.
                               - Akhlak: Meneladani akhlakul karimah dalam pergaulan global dan mempraktikkan moderasi beragama (wasathiyah).
                               - Fikih: Memahami zakat, infak, sedekah, kurban, dan makanan/minuman halal-haram.
                               - SPI: Meneladani perjuangan Fathu Makkah dan kepemimpinan Khulafaur Rasyidin.
                            
                            III. PENDEKATAN ASESMEN: TAKSONOMI SOLO
                            Penilaian dirancang berjenjang dari unistruktural, multistruktural, relasional, hingga extended abstract untuk mengukur tingkat pemahaman siswa secara komprehensif.
                        """.trimIndent()
                    )
                )
                repository.insertDocument(
                    MerdekaDocument(
                        type = "ATP",
                        title = "Alur Tujuan Pembelajaran PAI Fase B Kelas IV (CP 020/2026)",
                        classLevel = "IV",
                        contentText = """
                            ALUR TUJUAN PEMBELAJARAN (ATP) PAI KELAS IV BERBASIS SK BSKAP 020/2026
                            
                            Semester 1:
                            4.1. Membaca dan melafalkan Q.S. Al-Hujurat/49:13 secara tartil sesuai kaidah tajwid. (6 JP)
                            4.2. Memahami pesan pokok keragaman sebagai sunnatullah untuk saling mengenal (ta'aruf). (6 JP)
                            4.3. Mengidentifikasi asmaulhusna (Al-Malik, Al-Aziz, Al-Quddus, As-Salam, Al-Mu'min) dan penerapannya. (9 JP)
                            4.4. Menunjukkan sikap saling menghormati antarsesama pemeluk agama dan budaya yang berbeda. (6 JP)
                            4.5. Mengidentifikasi tanda-tanda usia baligh menurut ilmu fikih dan ilmu biologi. (9 JP)
                            
                            Semester 2:
                            4.6. Menjelaskan tata cara bersuci dari hadas besar (mandi wajib) beserta rukun dan sunahnya. (9 JP)
                            4.7. Menceritakan kisah keteladanan hijrah Nabi Muhammad SAW ke Madinah. (9 JP)
                            4.8. Mengamalkan persaudaraan kaum Muhajirin dan kaum Ansar dalam kehidupan sekolah. (6 JP)
                            4.9. Menjaga kebersihan dan kelestarian lingkungan sekolah sebagai manifestasi rasa syukur. (6 JP)
                        """.trimIndent()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearGeneratedResponse() {
        _aiGeneratedResponse.value = ""
    }

    // --- Teaching Journal Actions ---
    fun addJournal(date: String, classId: Int, className: String, topic: String, activity: String, teacherPresent: Boolean, presenceJson: String, location: String? = null, photo: String? = null) {
        if (!requireStaffPermission("menambah jurnal mengajar")) return
        if (topic.isBlank()) return
        viewModelScope.launch {
            val entity = TeachingJournal(
                date = date,
                classId = classId,
                className = className,
                topic = topic.trim(),
                activity = activity.trim(),
                teacherPresent = teacherPresent,
                studentPresenceJson = presenceJson,
                locationGps = location,
                photoPath = photo
            )
            repository.insertJournal(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.TEACHING_JOURNALS,
                entity.syncKey,
                syncEngine.encodeJournal(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah jurnal mengajar harian kelas: $className tentang: ${entity.topic}")
        }
    }

    fun updateJournal(journal: TeachingJournal) {
        if (!requireStaffPermission("mengubah jurnal mengajar")) return
        val updated = journal.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateJournal(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.TEACHING_JOURNALS,
                updated.syncKey,
                syncEngine.encodeJournal(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui jurnal mengajar tanggal ${updated.date} (${updated.className})")
        }
    }

    fun deleteJournal(journal: TeachingJournal) {
        if (!requireStaffPermission("menghapus jurnal mengajar")) return
        viewModelScope.launch {
            repository.deleteJournal(journal)
            syncEngine.enqueueDelete(SupabaseTables.TEACHING_JOURNALS, journal.syncKey)
            logActivity("Menghapus jurnal mengajar tanggal ${journal.date} (${journal.className})")
        }
    }

    // --- Assessment Records ---
    fun addAssessment(studentId: Int, studentName: String, classId: Int, type: String, topic: String, score: Int, description: String) {
        if (!requireStaffPermission("menambah penilaian")) return
        if (studentName.isBlank() || topic.isBlank()) return
        val safeScore = score.coerceIn(0, 100)
        viewModelScope.launch {
            val entity = AssessmentRecord(
                studentId = studentId,
                studentName = studentName.trim(),
                classId = classId,
                type = type,
                topic = topic.trim(),
                score = safeScore,
                description = description.trim()
            )
            repository.insertAssessment(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.ASSESSMENT_RECORDS,
                entity.syncKey,
                syncEngine.encodeAssessment(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah penilaian $type untuk siswa: $studentName (Nilai: $safeScore)")
        }
    }

    fun updateAssessment(record: AssessmentRecord) {
        if (!requireStaffPermission("mengubah penilaian")) return
        val updated = record.copy(score = record.score.coerceIn(0, 100), updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateAssessment(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.ASSESSMENT_RECORDS,
                updated.syncKey,
                syncEngine.encodeAssessment(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui penilaian siswa ${updated.studentName} (${updated.score})")
        }
    }

    fun deleteAssessment(record: AssessmentRecord) {
        if (!requireStaffPermission("menghapus penilaian")) return
        viewModelScope.launch {
            repository.deleteAssessment(record)
            syncEngine.enqueueDelete(SupabaseTables.ASSESSMENT_RECORDS, record.syncKey)
            logActivity("Menghapus penilaian siswa ${record.studentName}")
        }
    }

    // --- Questions Bank ---
    fun addQuestion(text: String, optionA: String, optionB: String, optionC: String, optionD: String, answerKey: String, difficulty: String, type: String, explanation: String) {
        if (!requireStaffPermission("menambah soal")) return
        if (text.isBlank()) return
        viewModelScope.launch {
            val entity = Question(
                text = text.trim(),
                optionA = optionA.trim(),
                optionB = optionB.trim(),
                optionC = optionC.trim(),
                optionD = optionD.trim(),
                answerKey = answerKey.trim(),
                difficulty = difficulty,
                type = type,
                explanation = explanation.trim()
            )
            repository.insertQuestion(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.QUESTIONS,
                entity.syncKey,
                syncEngine.encodeQuestion(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah soal baru ke bank soal ($difficulty)")
        }
    }

    fun updateQuestion(question: Question) {
        if (!requireStaffPermission("mengubah soal")) return
        val updated = question.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateQuestion(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.QUESTIONS,
                updated.syncKey,
                syncEngine.encodeQuestion(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui soal ID: ${updated.id}")
        }
    }

    fun deleteQuestion(question: Question) {
        if (!requireStaffPermission("menghapus soal")) return
        viewModelScope.launch {
            repository.deleteQuestion(question)
            syncEngine.enqueueDelete(SupabaseTables.QUESTIONS, question.syncKey)
            logActivity("Menghapus soal ID: ${question.id}")
        }
    }

    // --- Religious Activity Actions ---
    fun addReligiousActivity(title: String, date: String, description: String, type: String, participantCount: Int, photo: String? = null) {
        if (!requireStaffPermission("menambah kegiatan keagamaan")) return
        if (title.isBlank()) return
        viewModelScope.launch {
            val entity = ReligiousActivity(
                title = title.trim(),
                date = date,
                description = description.trim(),
                type = type,
                participantCount = participantCount.coerceAtLeast(0),
                photoPath = photo
            )
            repository.insertActivity(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.RELIGIOUS_ACTIVITIES,
                entity.syncKey,
                syncEngine.encodeReligiousActivity(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah kegiatan keagamaan: ${entity.title} ($type)")
        }
    }

    fun updateReligiousActivity(activity: ReligiousActivity) {
        if (!requireStaffPermission("mengubah kegiatan keagamaan")) return
        val updated = activity.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateActivity(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.RELIGIOUS_ACTIVITIES,
                updated.syncKey,
                syncEngine.encodeReligiousActivity(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui kegiatan keagamaan: ${updated.title}")
        }
    }

    fun deleteReligiousActivity(activity: ReligiousActivity) {
        if (!requireStaffPermission("menghapus kegiatan keagamaan")) return
        viewModelScope.launch {
            repository.deleteActivity(activity)
            syncEngine.enqueueDelete(SupabaseTables.RELIGIOUS_ACTIVITIES, activity.syncKey)
            logActivity("Menghapus kegiatan keagamaan: ${activity.title}")
        }
    }

    // --- Extracurricular Actions ---
    fun addExtracurricular(title: String, mentorName: String, schedule: String, count: Int, description: String) {
        if (!requireStaffPermission("menambah ekstrakurikuler")) return
        if (title.isBlank()) return
        viewModelScope.launch {
            val entity = Extracurricular(
                title = title.trim(),
                mentorName = mentorName.trim(),
                schedule = schedule.trim(),
                participantsCount = count.coerceAtLeast(0),
                description = description.trim()
            )
            repository.insertExtracurricular(entity)
            syncEngine.enqueueUpsert(
                SupabaseTables.EXTRACURRICULARS,
                entity.syncKey,
                syncEngine.encodeExtracurricular(entity, backendGateway.currentUserId)
            )
            logActivity("Menambah kegiatan ekstrakurikuler baru: ${entity.title}")
        }
    }

    fun updateExtracurricular(extracurricular: Extracurricular) {
        if (!requireStaffPermission("mengubah ekstrakurikuler")) return
        val updated = extracurricular.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.updateExtracurricular(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.EXTRACURRICULARS,
                updated.syncKey,
                syncEngine.encodeExtracurricular(updated, backendGateway.currentUserId)
            )
            logActivity("Memperbarui ekstrakurikuler: ${updated.title}")
        }
    }

    fun deleteExtracurricular(extracurricular: Extracurricular) {
        if (!requireStaffPermission("menghapus ekstrakurikuler")) return
        viewModelScope.launch {
            repository.deleteExtracurricular(extracurricular)
            syncEngine.enqueueDelete(SupabaseTables.EXTRACURRICULARS, extracurricular.syncKey)
            logActivity("Menghapus ekstrakurikuler: ${extracurricular.title}")
        }
    }

    // --- Import & Export / Backup & Restore System ---

    fun exportBackup(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = buildBackupJson()
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor cadangan sistem administrasi PAI ke perangkat")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun importBackup(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val jsonString = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonString != null) {
                    restoreBackupFromJson(jsonString)
                    logActivity("Mengimpor cadangan sistem administrasi PAI dari perangkat")
                    onSuccess()
                } else {
                    onError(Exception("File kosong atau tidak dapat dibaca"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportStudentsToJson(classId: Int?, uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val students = if (classId != null && classId != -1) {
                    allStudents.value.filter { it.classId == classId }
                } else {
                    allStudents.value
                }
                val classObj = classId?.let { cid -> allClasses.value.find { it.id == cid } }
                val className = classObj?.name ?: "Semua_Kelas"
                
                val json = org.json.JSONObject()
                json.put("school", "SD Negeri Kiarapandak 01")
                json.put("class_id", classId ?: 0)
                json.put("class_name", className)
                json.put("total_students", students.size)
                
                val array = org.json.JSONArray()
                for (s in students) {
                    val sClass = allClasses.value.find { it.id == s.classId }?.name ?: "Kelas"
                    array.put(org.json.JSONObject().apply {
                        put("nis", s.nis)
                        put("nisn", s.nisn)
                        put("name", s.name)
                        put("gender", s.gender)
                        put("className", sClass)
                        put("birthPlace", s.birthPlace)
                        put("birthDate", s.birthDate)
                        put("parentName", s.parentName)
                        put("parentPhone", s.parentPhone)
                        put("address", s.address)
                    })
                }
                json.put("students", array)
                
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toString(4).toByteArray(Charsets.UTF_8))
                }
                
                logActivity("Mengekspor ${students.size} data siswa ($className) ke perangkat")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportAssessmentsToCsv(classId: Int?, uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val records = if (classId != null && classId != -1) {
                    allAssessments.value.filter { it.classId == classId }
                } else {
                    allAssessments.value
                }
                val sb = StringBuilder()
                sb.append("No,Nama Siswa,Kelas,Jenis Penilaian,Tema / Materi,Nilai,Predikat,Catatan\n")
                records.forEachIndexed { index, r ->
                    val cName = allClasses.value.find { it.id == r.classId }?.name ?: "Kelas"
                    val predikat = when {
                        r.score >= 90 -> "A (Sangat Baik)"
                        r.score >= 80 -> "B (Baik)"
                        r.score >= 70 -> "C (Cukup)"
                        else -> "D (Perlu Bimbingan)"
                    }
                    val safeTopic = r.topic.replace("\"", "\"\"")
                    val safeDesc = r.description.replace("\"", "\"\"")
                    sb.append("${index + 1},\"${r.studentName}\",\"$cName\",\"${r.type}\",\"$safeTopic\",${r.score},\"$predikat\",\"$safeDesc\"\n")
                }

                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor ${records.size} data rekap penilaian siswa ke format CSV")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportRaporToHtml(classId: Int?, uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val records = if (classId != null && classId != -1) {
                    allAssessments.value.filter { it.classId == classId }
                } else {
                    allAssessments.value
                }
                val school = schoolData.value
                val teacher = teacherProfile.value
                val targetClassName = if (classId != null && classId != -1) {
                    allClasses.value.find { it.id == classId }?.name ?: "Semua Rombel"
                } else {
                    "Semua Rombel"
                }

                val dateNow = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
                val rowsHtml = StringBuilder()
                records.forEachIndexed { idx, r ->
                    val cName = allClasses.value.find { it.id == r.classId }?.name ?: "Kelas"
                    val predikat = when {
                        r.score >= 90 -> "A (Sangat Baik)"
                        r.score >= 80 -> "B (Baik)"
                        r.score >= 70 -> "C (Cukup)"
                        else -> "D (Perlu Bimbingan)"
                    }
                    rowsHtml.append("""
                        <tr>
                            <td style="text-align:center;">${idx + 1}</td>
                            <td><strong>${r.studentName}</strong></td>
                            <td style="text-align:center;">$cName</td>
                            <td>${r.type}</td>
                            <td>${r.topic}</td>
                            <td style="text-align:center; font-weight:bold; color:${if (r.score >= 75) "#2e7d32" else "#c62828"};">${r.score}</td>
                            <td style="text-align:center;">$predikat</td>
                            <td>${r.description.ifEmpty { "-" }}</td>
                        </tr>
                    """.trimIndent())
                }

                val html = """
                    <!DOCTYPE html>
                    <html lang="id">
                    <head>
                        <meta charset="UTF-8">
                        <title>Laporan Rapor Penilaian PAI - $targetClassName</title>
                        <style>
                            body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; color: #2c3e50; }
                            .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 12px; margin-bottom: 20px; }
                            .header h2 { margin: 0; color: #1b5e20; }
                            .header p { margin: 4px 0 0 0; color: #666; font-size: 13px; }
                            table { width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 13px; }
                            th, td { padding: 8px 10px; border: 1px solid #ccc; }
                            th { background-color: #e8f5e9; color: #2e7d32; font-weight: bold; }
                            .ttd-box { margin-top: 40px; display: flex; justify-content: space-between; }
                            .ttd { text-align: center; width: 220px; }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <h2>REKAPITULASI PENILAIAN & RAPOR PENDIDIKAN AGAMA ISLAM</h2>
                            <p>${school.schoolName} | Kelas: $targetClassName | Tahun Ajaran 2026/2027</p>
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th>No</th>
                                    <th>Nama Siswa</th>
                                    <th>Kelas</th>
                                    <th>Jenis Penilaian</th>
                                    <th>Tema / Materi</th>
                                    <th>Nilai</th>
                                    <th>Predikat</th>
                                    <th>Catatan Guru</th>
                                </tr>
                            </thead>
                            <tbody>
                                $rowsHtml
                            </tbody>
                        </table>
                        <div class="ttd-box">
                            <div class="ttd">
                                <p>Mengetahui,<br>Kepala Sekolah</p>
                                <br><br><br>
                                <strong><u>${school.headmasterName}</u></strong><br>
                                NIP. -
                            </div>
                            <div class="ttd">
                                <p>${school.address.ifEmpty { "Kiarapandak" }}, $dateNow<br>Guru PAI & Budi Pekerti,</p>
                                <br><br><br>
                                <strong><u>${teacher.fullName}</u></strong><br>
                                NIP. ${teacher.nip.ifEmpty { "-" }}
                            </div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(html.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mencetak rapor rekapitulasi penilaian PAI ke format HTML/PDF")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportKokurikulerToHtml(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val school = schoolData.value
                val teacher = teacherProfile.value
                val dateNow = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
                val html = """
                    <!DOCTYPE html>
                    <html lang="id">
                    <head>
                        <meta charset="UTF-8">
                        <title>Laporan Kegiatan Kokurikuler PAI</title>
                        <style>
                            body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; color: #333; line-height: 1.6; }
                            .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 12px; margin-bottom: 20px; }
                            .header h2 { margin: 0; color: #1b5e20; }
                            .card { border: 1px solid #ddd; border-radius: 8px; padding: 15px; margin-bottom: 15px; background: #fdfdfd; }
                            .badge { background: #e8f5e9; color: #2e7d32; font-weight: bold; padding: 3px 8px; border-radius: 4px; font-size: 12px; }
                            .ttd-box { margin-top: 40px; display: flex; justify-content: flex-end; }
                            .ttd { text-align: center; width: 250px; }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <h2>AGENDA & LAPORAN KEGIATAN KOKURIKULER PAI</h2>
                            <p>${school.schoolName} • Tahun Ajaran 2026/2027</p>
                        </div>
                        <div class="card">
                            <h3>1. Field Trip / Observasi Adab Masjid & Rumah Ibadah <span class="badge">Terjadwal</span></h3>
                            <p><strong>Deskripsi:</strong> Siswa diajak mengamati arsitektur masjid, musala, dan mengidentifikasi adab-adab bertamu serta kebersihan tempat ibadah.</p>
                            <p><strong>Jadwal:</strong> 25 Juli 2026 | <strong>Sasaran:</strong> Peserta Didik Kelas IV & V</p>
                        </div>
                        <div class="card">
                            <h3>2. Gerakan Sedekah Sampah Plastik <span class="badge">Rutin Mingguan</span></h3>
                            <p><strong>Deskripsi:</strong> Mengaplikasikan nilai iman kepada Allah SWT lewat pelestarian alam, kebersihan lingkungan, dan sedekah produktif.</p>
                            <p><strong>Jadwal:</strong> Rutin Setiap Hari Jumat | <strong>Sasaran:</strong> Seluruh Siswa SDN Kiarapandak 01</p>
                        </div>
                        <div class="ttd-box">
                            <div class="ttd">
                                <p>${school.address.ifEmpty { "Kiarapandak" }}, $dateNow<br>Guru PAI Pembina Kegiatan,</p>
                                <br><br><br>
                                <strong><u>${teacher.fullName}</u></strong><br>
                                NIP. ${teacher.nip.ifEmpty { "-" }}
                            </div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(html.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor laporan kegiatan kokurikuler ke format cetak HTML/PDF")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportP5ModulToHtml(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val school = schoolData.value
                val teacher = teacherProfile.value
                val dateNow = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
                val html = """
                    <!DOCTYPE html>
                    <html lang="id">
                    <head>
                        <meta charset="UTF-8">
                        <title>Modul Projek P5 & PPRA - PAI</title>
                        <style>
                            body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; color: #333; line-height: 1.6; }
                            .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 12px; margin-bottom: 20px; }
                            .header h2 { margin: 0; color: #1b5e20; }
                            .project-box { border: 1px solid #c8e6c9; background: #f9fbe7; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
                            .project-box h3 { color: #2e7d32; margin-top: 0; }
                            .ttd-box { margin-top: 40px; display: flex; justify-content: flex-end; }
                            .ttd { text-align: center; width: 250px; }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <h2>MODUL PERENCANAAN PROJEK P5 & PPRA</h2>
                            <p>Projek Penguatan Profil Pelajar Pancasila & Rahmatan Lil 'Alamin<br>${school.schoolName}</p>
                        </div>
                        <div class="project-box">
                            <h3>Tema 1: Berakhlak Mulia bagi Sesama (Akhlak kepada Manusia)</h3>
                            <p><strong>Sub-Tema:</strong> Indahnya Berbagi, Menghargai Perbedaan Suku</p>
                            <p><strong>Kelompok Sasaran:</strong> Fase B (Kelas IV)</p>
                            <p><strong>Dimensi Profil Pelajar:</strong> Beriman, Bertakwa kepada Tuhan YME, dan Berakhlak Mulia; Berkebinekaan Global; Gotong Royong.</p>
                            <p><strong>Bentuk Aksi Nyata:</strong> Bakti Sosial siswa ke lingkungan sekitar sekolah dan pembuatan infografis/poster toleransi keragaman suku bangsa.</p>
                        </div>
                        <div class="project-box">
                            <h3>Tema 2: Hidup Bersih Cermin Keimanan</h3>
                            <p><strong>Sub-Tema:</strong> Pengolahan Sampah Organik dan Kebersihan Musala Sekolah</p>
                            <p><strong>Kelompok Sasaran:</strong> Fase C (Kelas V)</p>
                            <p><strong>Dimensi Profil Pelajar:</strong> Bernalar Kritis, Mandiri, Peduli Lingkungan (Akhlak kepada Alam).</p>
                            <p><strong>Bentuk Aksi Nyata:</strong> Pembuatan kompos ramah lingkungan dari sisa dedaunan pohon sekolah.</p>
                        </div>
                        <div class="ttd-box">
                            <div class="ttd">
                                <p>${school.address.ifEmpty { "Kiarapandak" }}, $dateNow<br>Koordinator Fasilitator Projek,</p>
                                <br><br><br>
                                <strong><u>${teacher.fullName}</u></strong><br>
                                NIP. ${teacher.nip.ifEmpty { "-" }}
                            </div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(html.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor berkas modul perencanaan projek P5 + PPRA")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun importStudentsFromFile(classId: Int, uri: android.net.Uri, onSuccess: (Int) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content != null) {
                    val count = performImportStudents(classId, content)
                    val classObj = allClasses.value.find { it.id == classId }
                    val className = classObj?.name ?: "Kelas"
                    logActivity("Berhasil mengimpor $count siswa ke kelas $className")
                    onSuccess(count)
                } else {
                    onError(Exception("File kosong atau tidak dapat dibaca"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private suspend fun buildBackupJson(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val json = org.json.JSONObject()

        // 1. Teacher Profile
        val profile = teacherProfile.value
        val jProfile = org.json.JSONObject().apply {
            put("fullName", profile.fullName)
            put("nip", profile.nip)
            put("nuptk", profile.nuptk)
            put("certNo", profile.certNo)
            put("rank", profile.rank)
            put("education", profile.education)
            put("photoPath", profile.photoPath ?: "")
            put("digitalSignature", profile.digitalSignature ?: "")
            put("email", profile.email)
            put("phone", profile.phone)
            put("employmentStatus", profile.employmentStatus)
            put("subjectTaught", profile.subjectTaught)
            put("workUnit", profile.workUnit)
            put("additionalDuties", profile.additionalDuties)
            put("birthPlaceDate", profile.birthPlaceDate)
        }
        json.put("teacher_profile", jProfile)

        // 2. School Data
        val school = schoolData.value
        val jSchool = org.json.JSONObject().apply {
            put("schoolName", school.schoolName)
            put("npsn", school.npsn)
            put("address", school.address)
            put("headmasterName", school.headmasterName)
            put("academicYear", school.academicYear)
            put("semester", school.semester)
            put("curriculum", school.curriculum)
        }
        json.put("school_data", jSchool)

        // 3. ClassRooms
        val classes = allClasses.value
        val jClasses = org.json.JSONArray()
        for (c in classes) {
            jClasses.put(org.json.JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("level", c.level)
            })
        }
        json.put("class_rooms", jClasses)

        // 4. Students
        val students = allStudents.value
        val jStudents = org.json.JSONArray()
        for (s in students) {
            jStudents.put(org.json.JSONObject().apply {
                put("id", s.id)
                put("classId", s.classId)
                put("nis", s.nis)
                put("name", s.name)
                put("gender", s.gender)
                put("parentName", s.parentName)
                put("parentPhone", s.parentPhone)
            })
        }
        json.put("students", jStudents)

        // 5. Teaching Journals
        val journals = allJournals.value
        val jJournals = org.json.JSONArray()
        for (j in journals) {
            jJournals.put(org.json.JSONObject().apply {
                put("id", j.id)
                put("date", j.date)
                put("classId", j.classId)
                put("className", j.className)
                put("topic", j.topic)
                put("activity", j.activity)
                put("teacherPresent", j.teacherPresent)
                put("studentPresenceJson", j.studentPresenceJson)
                put("locationGps", j.locationGps ?: "")
                put("photoPath", j.photoPath ?: "")
            })
        }
        json.put("teaching_journals", jJournals)

        // 6. Assessment Records
        val assessments = allAssessments.value
        val jAssessments = org.json.JSONArray()
        for (a in assessments) {
            jAssessments.put(org.json.JSONObject().apply {
                put("id", a.id)
                put("studentId", a.studentId)
                put("studentName", a.studentName)
                put("classId", a.classId)
                put("type", a.type)
                put("topic", a.topic)
                put("score", a.score)
                put("description", a.description)
                put("date", a.date)
            })
        }
        json.put("assessment_records", jAssessments)

        // 7. Questions
        val questions = allQuestions.value
        val jQuestions = org.json.JSONArray()
        for (q in questions) {
            jQuestions.put(org.json.JSONObject().apply {
                put("id", q.id)
                put("text", q.text)
                put("optionA", q.optionA)
                put("optionB", q.optionB)
                put("optionC", q.optionC)
                put("optionD", q.optionD)
                put("answerKey", q.answerKey)
                put("difficulty", q.difficulty)
                put("type", q.type)
                put("explanation", q.explanation)
            })
        }
        json.put("questions", jQuestions)

        // 8. Religious Activities
        val activities = allActivities.value
        val jActivities = org.json.JSONArray()
        for (act in activities) {
            jActivities.put(org.json.JSONObject().apply {
                put("id", act.id)
                put("title", act.title)
                put("date", act.date)
                put("description", act.description)
                put("type", act.type)
                put("participantCount", act.participantCount)
                put("photoPath", act.photoPath ?: "")
            })
        }
        json.put("religious_activities", jActivities)

        // 9. Extracurriculars
        val extras = allExtracurriculars.value
        val jExtras = org.json.JSONArray()
        for (e in extras) {
            jExtras.put(org.json.JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("mentorName", e.mentorName)
                put("schedule", e.schedule)
                put("participantsCount", e.participantsCount)
                put("description", e.description)
            })
        }
        json.put("extracurriculars", jExtras)

        // 10. Merdeka Documents
        val docs = allDocuments.value
        val jDocs = org.json.JSONArray()
        for (d in docs) {
            jDocs.put(org.json.JSONObject().apply {
                put("id", d.id)
                put("type", d.type)
                put("title", d.title)
                put("subject", d.subject)
                put("classLevel", d.classLevel)
                put("contentText", d.contentText)
                put("createdAt", d.createdAt)
                put("fileUri", d.fileUri ?: "")
                put("fileName", d.fileName ?: "")
                put("fileSize", d.fileSize ?: "")
                put("fileMimeType", d.fileMimeType ?: "")
                put("semester", d.semester)
                put("academicYear", d.academicYear)
                put("authorTeacher", d.authorTeacher)
            })
        }
        json.put("merdeka_documents", jDocs)

        json.toString(4)
    }

    private suspend fun restoreBackupFromJson(jsonString: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val json = org.json.JSONObject(jsonString)
        
        // Clear the database safely
        db.clearAllTables()
        
        // 1. Restore Teacher Profile
        if (json.has("teacher_profile")) {
            val jProfile = json.getJSONObject("teacher_profile")
            val profile = TeacherProfile(
                id = 1,
                fullName = jProfile.optString("fullName", "Muhamad Suhendi, S.Pd"),
                nip = jProfile.optString("nip", "199308162024211007"),
                nuptk = jProfile.optString("nuptk", "0"),
                certNo = jProfile.optString("certNo", ""),
                rank = jProfile.optString("rank", "Ahli Pertama / IX"),
                education = jProfile.optString("education", "S1 Pendidikan Agama Islam"),
                photoPath = jProfile.optString("photoPath", "").let { if (it.isEmpty()) null else it },
                digitalSignature = jProfile.optString("digitalSignature", "").let { if (it.isEmpty()) null else it },
                email = jProfile.optString("email", "kkgpaikecamatansukajaya@gmail.com"),
                phone = jProfile.optString("phone", "081234567890"),
                employmentStatus = jProfile.optString("employmentStatus", "PPPK"),
                subjectTaught = jProfile.optString("subjectTaught", "Pendidikan Agama Islam dan Budi Pekerti"),
                workUnit = jProfile.optString("workUnit", "SD Negeri Kiarapandak 01"),
                additionalDuties = jProfile.optString("additionalDuties", "Pembina Rohis & Kegiatan Keagamaan"),
                birthPlaceDate = jProfile.optString("birthPlaceDate", "Bogor, 16 Agustus 1993")
            )
            db.teacherProfileDao().insertOrUpdateProfile(profile)
        } else {
            db.teacherProfileDao().insertOrUpdateProfile(TeacherProfile())
        }
        
        // 2. Restore School Data
        if (json.has("school_data")) {
            val jSchool = json.getJSONObject("school_data")
            val school = SchoolData(
                id = 1,
                schoolName = jSchool.optString("schoolName", ""),
                npsn = jSchool.optString("npsn", ""),
                address = jSchool.optString("address", ""),
                headmasterName = jSchool.optString("headmasterName", ""),
                academicYear = jSchool.optString("academicYear", ""),
                semester = jSchool.optString("semester", ""),
                curriculum = jSchool.optString("curriculum", "")
            )
            db.schoolDataDao().insertOrUpdateSchoolData(school)
        } else {
            db.schoolDataDao().insertOrUpdateSchoolData(SchoolData())
        }
        
        // 3. Restore ClassRooms
        if (json.has("class_rooms")) {
            val jClasses = json.getJSONArray("class_rooms")
            for (i in 0 until jClasses.length()) {
                val jClass = jClasses.getJSONObject(i)
                val classRoom = ClassRoom(
                    id = jClass.optInt("id", 0),
                    name = jClass.getString("name"),
                    level = jClass.getString("level")
                )
                db.classRoomDao().insertClass(classRoom)
            }
        }
        
        // 4. Restore Students
        if (json.has("students")) {
            val jStudents = json.getJSONArray("students")
            for (i in 0 until jStudents.length()) {
                val jStudent = jStudents.getJSONObject(i)
                val student = Student(
                    id = jStudent.optInt("id", 0),
                    classId = jStudent.getInt("classId"),
                    nis = jStudent.getString("nis"),
                    name = jStudent.getString("name"),
                    gender = jStudent.getString("gender"),
                    parentName = jStudent.optString("parentName", ""),
                    parentPhone = jStudent.optString("parentPhone", "")
                )
                db.studentDao().insertStudent(student)
            }
        }
        
        // 5. Restore Teaching Journals
        if (json.has("teaching_journals")) {
            val jJournals = json.getJSONArray("teaching_journals")
            for (i in 0 until jJournals.length()) {
                val jJournal = jJournals.getJSONObject(i)
                val journal = TeachingJournal(
                    id = jJournal.optInt("id", 0),
                    date = jJournal.getString("date"),
                    classId = jJournal.getInt("classId"),
                    className = jJournal.getString("className"),
                    topic = jJournal.getString("topic"),
                    activity = jJournal.getString("activity"),
                    teacherPresent = jJournal.optBoolean("teacherPresent", true),
                    studentPresenceJson = jJournal.getString("studentPresenceJson"),
                    locationGps = jJournal.optString("locationGps", "").let { if (it.isEmpty()) null else it },
                    photoPath = jJournal.optString("photoPath", "").let { if (it.isEmpty()) null else it }
                )
                db.teachingJournalDao().insertJournal(journal)
            }
        }
        
        // 6. Restore Assessment Records
        if (json.has("assessment_records")) {
            val jAssessments = json.getJSONArray("assessment_records")
            for (i in 0 until jAssessments.length()) {
                val jAssessment = jAssessments.getJSONObject(i)
                val record = AssessmentRecord(
                    id = jAssessment.optInt("id", 0),
                    studentId = jAssessment.getInt("studentId"),
                    studentName = jAssessment.getString("studentName"),
                    classId = jAssessment.getInt("classId"),
                    type = jAssessment.getString("type"),
                    topic = jAssessment.getString("topic"),
                    score = jAssessment.getInt("score"),
                    description = jAssessment.getString("description"),
                    date = jAssessment.optLong("date", System.currentTimeMillis())
                )
                db.assessmentRecordDao().insertAssessment(record)
            }
        }
        
        // 7. Restore Questions
        if (json.has("questions")) {
            val jQuestions = json.getJSONArray("questions")
            for (i in 0 until jQuestions.length()) {
                val jQuestion = jQuestions.getJSONObject(i)
                val question = Question(
                    id = jQuestion.optInt("id", 0),
                    text = jQuestion.getString("text"),
                    optionA = jQuestion.getString("optionA"),
                    optionB = jQuestion.getString("optionB"),
                    optionC = jQuestion.getString("optionC"),
                    optionD = jQuestion.getString("optionD"),
                    answerKey = jQuestion.getString("answerKey"),
                    difficulty = jQuestion.getString("difficulty"),
                    type = jQuestion.getString("type"),
                    explanation = jQuestion.optString("explanation", "")
                )
                db.questionDao().insertQuestion(question)
            }
        }
        
        // 8. Restore Religious Activities
        if (json.has("religious_activities")) {
            val jActivities = json.getJSONArray("religious_activities")
            for (i in 0 until jActivities.length()) {
                val jActivity = jActivities.getJSONObject(i)
                val activity = ReligiousActivity(
                    id = jActivity.optInt("id", 0),
                    title = jActivity.getString("title"),
                    date = jActivity.getString("date"),
                    description = jActivity.getString("description"),
                    type = jActivity.getString("type"),
                    participantCount = jActivity.optInt("participantCount", 0),
                    photoPath = jActivity.optString("photoPath", "").let { if (it.isEmpty()) null else it }
                )
                db.religiousActivityDao().insertActivity(activity)
            }
        }
        
        // 9. Restore Extracurriculars
        if (json.has("extracurriculars")) {
            val jExtras = json.getJSONArray("extracurriculars")
            for (i in 0 until jExtras.length()) {
                val jExtra = jExtras.getJSONObject(i)
                val extra = Extracurricular(
                    id = jExtra.optInt("id", 0),
                    title = jExtra.getString("title"),
                    mentorName = jExtra.getString("mentorName"),
                    schedule = jExtra.getString("schedule"),
                    participantsCount = jExtra.optInt("participantsCount", 0),
                    description = jExtra.getString("description")
                )
                db.extracurricularDao().insertExtracurricular(extra)
            }
        }

        // 10. Restore Merdeka Documents
        if (json.has("merdeka_documents")) {
            val jDocs = json.getJSONArray("merdeka_documents")
            for (i in 0 until jDocs.length()) {
                val jDoc = jDocs.getJSONObject(i)
                val doc = MerdekaDocument(
                    id = jDoc.optInt("id", 0),
                    type = jDoc.getString("type"),
                    title = jDoc.getString("title"),
                    subject = jDoc.optString("subject", "Pendidikan Agama Islam dan Budi Pekerti"),
                    classLevel = jDoc.getString("classLevel"),
                    contentText = jDoc.getString("contentText"),
                    createdAt = jDoc.optLong("createdAt", System.currentTimeMillis()),
                    fileUri = jDoc.optString("fileUri", "").let { if (it.isEmpty()) null else it },
                    fileName = jDoc.optString("fileName", "").let { if (it.isEmpty()) null else it },
                    fileSize = jDoc.optString("fileSize", "").let { if (it.isEmpty()) null else it },
                    fileMimeType = jDoc.optString("fileMimeType", "").let { if (it.isEmpty()) null else it },
                    semester = jDoc.optString("semester", "Ganjil"),
                    academicYear = jDoc.optString("academicYear", "2026/2027"),
                    authorTeacher = jDoc.optString("authorTeacher", "Muhamad Suhendi, S.Pd")
                )
                db.merdekaDocumentDao().insertDocument(doc)
            }
        }
    }

    private suspend fun performImportStudents(classId: Int, content: String): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var count = 0
        val trimmed = content.trim()
        
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            // Parse as JSON
            val json = if (trimmed.startsWith("{")) {
                val obj = org.json.JSONObject(trimmed)
                if (obj.has("students")) obj.getJSONArray("students") else org.json.JSONArray()
            } else {
                org.json.JSONArray(trimmed)
            }
            
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val student = Student(
                    classId = classId,
                    nis = item.optString("nis", ""),
                    name = item.optString("name", "Tanpa Nama"),
                    gender = item.optString("gender", "L"),
                    parentName = item.optString("parentName", ""),
                    parentPhone = item.optString("parentPhone", ""),
                    nisn = item.optString("nisn", ""),
                    birthPlace = item.optString("birthPlace", ""),
                    birthDate = item.optString("birthDate", ""),
                    address = item.optString("address", "")
                )
                db.studentDao().insertStudent(student)
                count++
            }
        } else {
            // Parse as CSV/Semicolon line-by-line
            val lines = trimmed.split("\n")
            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue
                
                // Skip header if it contains obvious keywords
                if (trimmedLine.lowercase().contains("nis") || trimmedLine.lowercase().contains("nama") || trimmedLine.lowercase().contains("gender")) {
                    continue
                }
                
                // Split by comma, semicolon, or tab
                val parts = when {
                    trimmedLine.contains(";") -> trimmedLine.split(";")
                    trimmedLine.contains("\t") -> trimmedLine.split("\t")
                    else -> trimmedLine.split(",")
                }
                
                if (parts.isNotEmpty()) {
                    val nis = parts.getOrNull(0)?.trim() ?: ""
                    val name = parts.getOrNull(1)?.trim() ?: "Siswa Baru"
                    val gender = parts.getOrNull(2)?.trim()?.uppercase() ?: "L"
                    val parentName = parts.getOrNull(3)?.trim() ?: ""
                    val parentPhone = parts.getOrNull(4)?.trim() ?: ""
                    
                    val student = Student(
                        classId = classId,
                        nis = nis,
                        name = name,
                        gender = if (gender.startsWith("P")) "P" else "L",
                        parentName = parentName,
                        parentPhone = parentPhone
                    )
                    db.studentDao().insertStudent(student)
                    count++
                }
            }
        }
        count
    }

    fun loadFullDapodikStudents(force: Boolean = false, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val jsonString = context.assets.open("students_dapodik.json").bufferedReader().use { it.readText() }
                val rootObj = org.json.JSONObject(jsonString)
                val classesArray = rootObj.getJSONArray("classes")
                val classIdMap = mutableMapOf<String, Int>()

                val existingClasses = repository.getAllClassesList()
                for (c in existingClasses) {
                    classIdMap[c.name] = c.id
                }

                for (i in 0 until classesArray.length()) {
                    val cObj = classesArray.getJSONObject(i)
                    val name = cObj.getString("name")
                    val level = cObj.getString("level")
                    if (!classIdMap.containsKey(name)) {
                        val id = repository.insertClass(ClassRoom(name = name, level = level))
                        classIdMap[name] = id.toInt()
                    }
                }

                if (force) {
                    repository.deleteAllStudents()
                }

                val studentsArray = rootObj.getJSONArray("students")
                val studentList = mutableListOf<Student>()
                for (i in 0 until studentsArray.length()) {
                    val sObj = studentsArray.getJSONObject(i)
                    val cName = sObj.getString("className")
                    val classId = classIdMap[cName] ?: (classIdMap.values.firstOrNull() ?: 1)
                    studentList.add(
                        Student(
                            classId = classId,
                            nis = sObj.optString("nis", ""),
                            name = sObj.getString("name"),
                            gender = sObj.optString("gender", "L"),
                            parentName = sObj.optString("parentName", "Orang Tua"),
                            parentPhone = sObj.optString("parentPhone", "-"),
                            nisn = sObj.optString("nisn", ""),
                            birthPlace = sObj.optString("birthPlace", ""),
                            birthDate = sObj.optString("birthDate", ""),
                            address = sObj.optString("address", "")
                        )
                    )
                }
                repository.insertStudents(studentList)
                logActivity("Sinkronisasi Dapodik: Berhasil memuat ${studentList.size} data siswa SD Negeri Kiarapandak 01")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete?.invoke(studentList.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete?.invoke(0)
                }
            }
        }
    }

    fun importStudentsFromText(classId: Int, text: String, onSuccess: (Int) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                if (text.trim().isEmpty()) {
                    onError(Exception("Teks kosong! Silakan masukkan data siswa."))
                    return@launch
                }
                val count = performImportStudents(classId, text)
                val classObj = allClasses.value.find { it.id == classId }
                val className = classObj?.name ?: "Kelas"
                logActivity("Berhasil mengimpor $count siswa ke kelas $className secara manual")
                onSuccess(count)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportTeacherProfileToHtml(teacher: TeacherProfile, school: SchoolData, uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val html = buildTeacherProfileHtml(teacher, school)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(html.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor Biodata Profil Guru (${teacher.fullName}) ke perangkat")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun buildTeacherProfileHtml(teacher: TeacherProfile, school: SchoolData): String {
        val dateNow = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <title>Biodata Administrasi Guru - ${teacher.fullName}</title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; color: #2c3e50; line-height: 1.6; }
                    .card { border: 2px solid #2e7d32; border-radius: 10px; padding: 25px; max-width: 800px; margin: auto; }
                    .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 15px; margin-bottom: 20px; }
                    .header h2 { margin: 0; color: #1b5e20; }
                    .header p { margin: 4px 0 0 0; color: #666; font-size: 14px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { padding: 10px 12px; border-bottom: 1px solid #e0e0e0; text-align: left; font-size: 14px; }
                    th { width: 35%; background-color: #f1f8e9; color: #2e7d32; }
                    .ttd-box { margin-top: 30px; display: flex; justify-content: flex-end; }
                    .ttd { text-align: center; width: 250px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h2>BIODATA ADMINISTRASI GURU PAI</h2>
                        <p>${school.schoolName} | NPSN: ${school.npsn}</p>
                    </div>
                    <table>
                        <tr><th>Nama Lengkap & Gelar</th><td><strong>${teacher.fullName}</strong></td></tr>
                        <tr><th>NIP</th><td>${teacher.nip.ifEmpty { "-" }}</td></tr>
                        <tr><th>NUPTK</th><td>${teacher.nuptk.ifEmpty { "-" }}</td></tr>
                        <tr><th>No. Sertifikat Pendidik / NRG</th><td>${teacher.certNo.ifEmpty { "-" }}</td></tr>
                        <tr><th>Pangkat / Golongan</th><td>${teacher.rank.ifEmpty { "-" }}</td></tr>
                        <tr><th>Status Kepegawaian</th><td>${teacher.employmentStatus}</td></tr>
                        <tr><th>Pendidikan Terakhir</th><td>${teacher.education.ifEmpty { "-" }}</td></tr>
                        <tr><th>Mata Pelajaran Diampu</th><td>${teacher.subjectTaught}</td></tr>
                        <tr><th>Unit Kerja</th><td>${teacher.workUnit}</td></tr>
                        <tr><th>Tugas Tambahan</th><td>${teacher.additionalDuties}</td></tr>
                        <tr><th>Tempat, Tanggal Lahir</th><td>${teacher.birthPlaceDate}</td></tr>
                        <tr><th>Email Dinas</th><td>${teacher.email}</td></tr>
                        <tr><th>No. Telepon / WhatsApp</th><td>${teacher.phone}</td></tr>
                    </table>
                    <div class="ttd-box">
                        <div class="ttd">
                            <p>${school.address.ifEmpty { "Sukajaya" }}, $dateNow<br>Guru Pendidikan Agama Islam,</p>
                            <br><br><br>
                            <strong><u>${teacher.fullName}</u></strong><br>
                            NIP. ${teacher.nip.ifEmpty { "-" }}
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun exportDocument(doc: MerdekaDocument, uri: android.net.Uri, format: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val contentToWrite = when (format.lowercase()) {
                    "html" -> buildHtmlDocument(doc)
                    else -> buildTextDocument(doc)
                }
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(contentToWrite.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor dokumen '${doc.title}' (${doc.type}) ke perangkat")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun buildTextDocument(doc: MerdekaDocument): String {
        val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(doc.createdAt))
        return """
            DOKUMEN ADMINISTRASI PEMBELAJARAN PENDIDIKAN AGAMA ISLAM
            ==================================================
            Jenis Dokumen : ${doc.type}
            Judul         : ${doc.title}
            Mata Pelajaran: ${doc.subject}
            Kelas/Tingkat : Kelas ${doc.classLevel}
            Dibuat Pada   : $dateStr
            ==================================================
            
            ${doc.contentText}
        
        
        """.trimIndent()
    }

    private fun buildHtmlDocument(doc: MerdekaDocument): String {
        val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(doc.createdAt))
        val formattedContent = doc.contentText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>")

        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${doc.title}</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333333;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f7f9fa;
                    }
                    .document-card {
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
                        padding: 40px;
                        border: 1px solid #e1e8ed;
                        margin-bottom: 20px;
                        position: relative;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 3px double #2e7d32;
                        padding-bottom: 15px;
                        margin-bottom: 25px;
                    }
                    .header h1 {
                        color: #2e7d32;
                        margin: 0;
                        font-size: 24px;
                        text-transform: uppercase;
                    }
                    .header p {
                        margin: 5px 0 0 0;
                        color: #666;
                        font-size: 12px;
                        letter-spacing: 1px;
                    }
                    .meta-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 25px;
                        background-color: #f9f9f9;
                    }
                    .meta-table td {
                        padding: 8px 12px;
                        border: 1px solid #e0e0e0;
                        font-size: 14px;
                    }
                    .meta-table td.label {
                        font-weight: bold;
                        color: #555;
                        width: 30%;
                    }
                    .content {
                        font-size: 15px;
                        white-space: pre-wrap;
                        word-break: break-word;
                        text-align: justify;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 40px;
                        padding-top: 15px;
                        border-top: 1px solid #e0e0e0;
                        font-size: 11px;
                        color: #888;
                    }
                    .actions {
                        text-align: center;
                        margin-bottom: 20px;
                    }
                    .btn {
                        background-color: #2e7d32;
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        font-size: 14px;
                        font-weight: bold;
                        border-radius: 4px;
                        cursor: pointer;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        transition: background-color 0.2s;
                    }
                    .btn:hover {
                        background-color: #1b5e20;
                    }
                    @media print {
                        body {
                            background-color: #ffffff;
                            padding: 0;
                        }
                        .document-card {
                            border: none;
                            box-shadow: none;
                            padding: 0;
                        }
                        .actions {
                            display: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="actions">
                    <button class="btn" onclick="window.print()">Cetak / Simpan ke PDF</button>
                </div>
                <div class="document-card">
                    <div class="header">
                        <h1>Perangkat Pembelajaran PAI</h1>
                        <p>Kementerian Pendidikan, Kebudayaan, Riset, dan Teknologi • Kurikulum Merdeka</p>
                    </div>
                    
                    <table class="meta-table">
                        <tr>
                            <td class="label">Jenis Perangkat</td>
                            <td>${doc.type}</td>
                        </tr>
                        <tr>
                            <td class="label">Judul Dokumen</td>
                            <td><strong>${doc.title}</strong></td>
                        </tr>
                        <tr>
                            <td class="label">Mata Pelajaran</td>
                            <td>${doc.subject}</td>
                        </tr>
                        <tr>
                            <td class="label">Kelas / Tingkat</td>
                            <td>Kelas ${doc.classLevel}</td>
                        </tr>
                        <tr>
                            <td class="label">Tanggal Dibuat</td>
                            <td>$dateStr</td>
                        </tr>
                    </table>
                    
                    <div class="content">${doc.contentText}</div>
                    
                    <div class="footer">
                        Dokumen ini dihasilkan secara otomatis oleh SIM Administrasi Guru PAI Kecamatan Sukajaya<br>
                        &copy; 2026 KKG PAI Kecamatan Sukajaya. All Rights Reserved.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun exportJournalReportToWord(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val journals = allJournals.value
                val school = schoolData.value
                val teacher = teacherProfile.value
                val htmlContent = buildJournalReportHtml(journals, school, teacher)
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor Rekap Jurnal Harian Mengajar ke format Word (.doc)")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun buildJournalReportHtml(journals: List<TeachingJournal>, school: SchoolData, teacher: TeacherProfile): String {
        val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        val journalRows = java.lang.StringBuilder()
        journals.forEachIndexed { idx, j ->
            journalRows.append("""
                <tr>
                    <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${idx + 1}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">${j.date}</td>
                    <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${j.className}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;"><b>${j.topic}</b></td>
                    <td style="border: 1px solid #ddd; padding: 8px;">${j.activity}</td>
                    <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${if (j.teacherPresent) "Hadir" else "Tidak Hadir"}</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
            <head>
                <meta charset="utf-8">
                <title>Laporan Jurnal Harian Guru PAI</title>
                <style>
                    body { font-family: 'Times New Roman', Times, serif; font-size: 12pt; line-height: 1.5; color: #000; }
                    .kop-surat { text-align: center; border-bottom: 3px double #000; padding-bottom: 10px; margin-bottom: 20px; }
                    .kop-surat h2 { margin: 0; font-size: 16pt; font-weight: bold; text-transform: uppercase; }
                    .kop-surat h3 { margin: 5px 0 0 0; font-size: 14pt; font-weight: bold; }
                    .kop-surat p { margin: 5px 0 0 0; font-size: 10pt; font-style: italic; }
                    .title { text-align: center; font-weight: bold; font-size: 14pt; text-transform: uppercase; margin-bottom: 20px; text-decoration: underline; }
                    .meta-info { width: 100%; margin-bottom: 20px; }
                    .meta-info td { padding: 3px 0; }
                    .meta-info td.label { width: 180px; font-weight: bold; }
                    .report-table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    .report-table th { border: 1px solid #000; background-color: #f2f2f2; padding: 8px; font-weight: bold; text-align: center; }
                    .report-table td { border: 1px solid #000; padding: 8px; vertical-align: top; }
                    .signature-section { width: 100%; margin-top: 40px; }
                    .signature-table { width: 100%; border: none; }
                    .signature-table td { width: 50%; text-align: center; border: none; padding-top: 15px; }
                </style>
            </head>
            <body>
                <div class="kop-surat">
                    <h2>KEMENTERIAN PENDIDIKAN, KEBUDAYAAN, RISET, DAN TEKNOLOGI</h2>
                    <h3>${school.schoolName.uppercase()}</h3>
                    <p>Alamat: ${school.address} | NPSN: ${school.npsn}</p>
                </div>
                
                <div class="title">LAPORAN REKAPITULASI JURNAL HARIAN MENGAJAR GURU PAI</div>
                
                <table class="meta-info">
                    <tr>
                        <td class="label">Nama Guru</td>
                        <td>: ${teacher.fullName}</td>
                    </tr>
                    <tr>
                        <td class="label">NIP / NUPTK</td>
                        <td>: ${if (teacher.nip.isNotEmpty()) teacher.nip else "-"} / ${if (teacher.nuptk.isNotEmpty()) teacher.nuptk else "-"}</td>
                    </tr>
                    <tr>
                        <td class="label">Tahun Ajaran / Semester</td>
                        <td>: ${school.academicYear} / ${school.semester}</td>
                    </tr>
                    <tr>
                        <td class="label">Kurikulum</td>
                        <td>: ${school.curriculum}</td>
                    </tr>
                </table>
                
                <table class="report-table">
                    <thead>
                        <tr>
                            <th style="width: 5%;">No</th>
                            <th style="width: 15%;">Tanggal</th>
                            <th style="width: 10%;">Kelas</th>
                            <th style="width: 25%;">Topik Pembelajaran</th>
                            <th style="width: 35%;">Kegiatan Pembelajaran</th>
                            <th style="width: 10%;">Status Guru</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${if (journals.isEmpty()) "<tr><td colspan='6' style='text-align:center; padding: 15px;'>Belum ada data jurnal mengajar</td></tr>" else journalRows.toString()}
                    </tbody>
                </table>
                
                <div class="signature-section">
                    <table class="signature-table">
                        <tr>
                            <td>
                                Mengetahui,<br>
                                Kepala Sekolah ${school.schoolName}<br><br><br><br>
                                <b><u>${school.headmasterName.ifEmpty { "DIAN NURHAYATI, S.Pd MM" }}</u></b><br>
                                NIP. _____________________
                            </td>
                            <td>
                                Sukajaya, $dateStr<br>
                                Guru PAI dan Budi Pekerti<br><br><br><br>
                                <b><u>${teacher.fullName.ifEmpty { "Muhamad Suhendi, S.Pd" }}</u></b><br>
                                NIP. ${teacher.nip.ifEmpty { "1993081620242112007" }}
                            </td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun exportAssessmentReportToExcel(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val assessments = allAssessments.value
                val classes = allClasses.value
                val csvContent = buildAssessmentReportCsv(assessments, classes)
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // Write BOM first to ensure Excel parses UTF-8 correctly
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor Rekap Penilaian Siswa ke format Excel (.csv)")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun buildAssessmentReportCsv(assessments: List<AssessmentRecord>, classes: List<ClassRoom>): String {
        val sb = java.lang.StringBuilder()
        sb.append("REKAPITULASI PENILAIAN SISWA PAI\n")
        sb.append("Sistem Informasi Manajemen Administrasi Guru PAI\n")
        sb.append("Tanggal Ekspor:;${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
        
        sb.append("No;Nama Siswa;Kelas;Jenis Penilaian;Topik/Materi;Skor/Nilai;Deskripsi;Tanggal Penilaian\n")
        
        assessments.forEachIndexed { index, record ->
            val className = classes.find { it.id == record.classId }?.name ?: "Kelas ${record.classId}"
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(record.date))
            
            // Clean text from semicolons to prevent column shifting
            val cleanName = record.studentName.replace(";", ",")
            val cleanTopic = record.topic.replace(";", ",")
            val cleanDesc = record.description.replace(";", ",")
            
            sb.append("${index + 1};$cleanName;$className;${record.type};$cleanTopic;${record.score};$cleanDesc;$dateStr\n")
        }
        return sb.toString()
    }

    fun exportActivityReportToPdf(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val activities = allActivities.value
                val extracurriculars = allExtracurriculars.value
                val school = schoolData.value
                val teacher = teacherProfile.value
                val htmlContent = buildActivityReportHtml(activities, extracurriculars, school, teacher)
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengekspor Laporan Kegiatan Keagamaan & Ekstrakurikuler ke PDF/HTML")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun buildActivityReportHtml(activities: List<ReligiousActivity>, extracurriculars: List<Extracurricular>, school: SchoolData, teacher: TeacherProfile): String {
        val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        
        val actRows = java.lang.StringBuilder()
        activities.forEachIndexed { idx, act ->
            actRows.append("""
                <tr>
                    <td style="border: 1px solid #2e7d32; padding: 8px; text-align: center;">${idx + 1}</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;">${act.date}</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;"><b>${act.title}</b></td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;">${act.type}</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px; text-align: center;">${act.participantCount} Siswa</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;">${act.description}</td>
                </tr>
            """.trimIndent())
        }

        val extraRows = java.lang.StringBuilder()
        extracurriculars.forEachIndexed { idx, ex ->
            extraRows.append("""
                <tr>
                    <td style="border: 1px solid #2e7d32; padding: 8px; text-align: center;">${idx + 1}</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;"><b>${ex.title}</b></td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;">${ex.mentorName}</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;">${ex.schedule}</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px; text-align: center;">${ex.participantsCount} Siswa</td>
                    <td style="border: 1px solid #2e7d32; padding: 8px;">${ex.description}</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <title>Laporan Kegiatan Keagamaan dan Ekstrakurikuler PAI</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333333;
                        max-width: 900px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f4f6f8;
                    }
                    .document-card {
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
                        padding: 40px;
                        border: 1px solid #e1e8ed;
                        margin-bottom: 20px;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 3px double #2e7d32;
                        padding-bottom: 15px;
                        margin-bottom: 25px;
                    }
                    .header h1 {
                        color: #2e7d32;
                        margin: 0;
                        font-size: 22px;
                        text-transform: uppercase;
                    }
                    .header p {
                        margin: 5px 0 0 0;
                        color: #666;
                        font-size: 12px;
                    }
                    .title {
                        text-align: center;
                        font-weight: bold;
                        font-size: 16px;
                        color: #1b5e20;
                        text-transform: uppercase;
                        margin-bottom: 25px;
                        letter-spacing: 0.5px;
                    }
                    .meta-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 30px;
                    }
                    .meta-table td {
                        padding: 6px 12px;
                        border: none;
                        font-size: 14px;
                    }
                    .meta-table td.label {
                        font-weight: bold;
                        color: #2e7d32;
                        width: 25%;
                    }
                    .section-title {
                        font-size: 15px;
                        font-weight: bold;
                        color: #1b5e20;
                        border-left: 4px solid #2e7d32;
                        padding-left: 10px;
                        margin-top: 30px;
                        margin-bottom: 15px;
                        text-transform: uppercase;
                    }
                    .report-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                        font-size: 13px;
                    }
                    .report-table th {
                        border: 1px solid #2e7d32;
                        background-color: #e8f5e9;
                        color: #1b5e20;
                        padding: 10px;
                        font-weight: bold;
                        text-align: center;
                    }
                    .report-table td {
                        border: 1px solid #a5d6a7;
                        padding: 8px;
                        vertical-align: top;
                    }
                    .signature-table {
                        width: 100%;
                        border: none;
                        margin-top: 40px;
                        font-size: 14px;
                    }
                    .signature-table td {
                        width: 50%;
                        text-align: center;
                        border: none;
                        padding-top: 15px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 40px;
                        padding-top: 15px;
                        border-top: 1px solid #e0e0e0;
                        font-size: 11px;
                        color: #888;
                    }
                    .actions {
                        text-align: center;
                        margin-bottom: 20px;
                    }
                    .btn {
                        background-color: #2e7d32;
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        font-size: 14px;
                        font-weight: bold;
                        border-radius: 4px;
                        cursor: pointer;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        transition: background-color 0.2s;
                    }
                    .btn:hover {
                        background-color: #1b5e20;
                    }
                    @media print {
                        body {
                            background-color: #ffffff;
                            padding: 0;
                        }
                        .document-card {
                            border: none;
                            box-shadow: none;
                            padding: 0;
                        }
                        .actions {
                            display: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="actions">
                    <button class="btn" onclick="window.print()">Cetak / Simpan ke PDF</button>
                </div>
                <div class="document-card">
                    <div class="header">
                        <h1>${school.schoolName.uppercase()}</h1>
                        <p>Alamat: ${school.address} | NPSN: ${school.npsn}</p>
                    </div>
                    
                    <div class="title">Laporan Kegiatan Keagamaan Islam & Ekstrakurikuler PAI</div>
                    
                    <table class="meta-table">
                        <tr>
                            <td class="label">Nama Pembina/Guru</td>
                            <td>: ${teacher.fullName}</td>
                        </tr>
                        <tr>
                            <td class="label">NIP</td>
                            <td>: ${teacher.nip.ifEmpty { "-" }}</td>
                        </tr>
                        <tr>
                            <td class="label">Tahun Pelajaran</td>
                            <td>: ${school.academicYear} (Semester ${school.semester})</td>
                        </tr>
                    </table>
                    
                    <div class="section-title">A. Kegiatan Keagamaan Islam (Rohis & Pembiasaan)</div>
                    <table class="report-table">
                        <thead>
                            <tr>
                                <th style="width: 5%;">No</th>
                                <th style="width: 15%;">Tanggal</th>
                                <th style="width: 25%;">Nama Kegiatan</th>
                                <th style="width: 15%;">Jenis Kegiatan</th>
                                <th style="width: 15%;">Partisipasi</th>
                                <th style="width: 25%;">Keterangan / Deskripsi</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${if (activities.isEmpty()) "<tr><td colspan='6' style='text-align:center; padding: 15px;'>Belum ada data kegiatan keagamaan</td></tr>" else actRows.toString()}
                        </tbody>
                    </table>
                    
                    <div class="section-title">B. Ekstrakurikuler PAI (Tahfidz, Hadrah, dll)</div>
                    <table class="report-table">
                        <thead>
                            <tr>
                                <th style="width: 5%;">No</th>
                                <th style="width: 20%;">Nama Ekstrakurikuler</th>
                                <th style="width: 20%;">Guru Pembimbing</th>
                                <th style="width: 15%;">Jadwal Kegiatan</th>
                                <th style="width: 15%;">Anggota Aktif</th>
                                <th style="width: 25%;">Deskripsi Singkat</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${if (extracurriculars.isEmpty()) "<tr><td colspan='6' style='text-align:center; padding: 15px;'>Belum ada data ekstrakurikuler</td></tr>" else extraRows.toString()}
                        </tbody>
                    </table>
                    
                    <table class="signature-table">
                        <tr>
                            <td>
                                Mengetahui,<br>
                                Kepala Sekolah ${school.schoolName}<br><br><br><br>
                                <b><u>${school.headmasterName.ifEmpty { "Dian Nurhayati, S.Pd" }}</u></b><br>
                                NIP. 197011011991032002
                            </td>
                            <td>
                                Sukajaya, $dateStr<br>
                                Guru PAI dan Budi Pekerti<br><br><br><br>
                                <b><u>${teacher.fullName.ifEmpty { "Muhamad Suhendi, S.Pd" }}</u></b><br>
                                NIP. ${teacher.nip.ifEmpty { "199308162024211007" }}
                            </td>
                        </tr>
                    </table>
                    
                    <div class="footer">
                        Dokumen ini dihasilkan secara otomatis oleh MUhamad Suhendi<br>
                        &copy; 2026 KKG PAI Kecamatan Sukajaya. All Rights Reserved.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun exportExcelTemplate(uri: android.net.Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val templateContent = "NIS;Nama;Gender (L/P);Nama Orang Tua;No HP\n10243;Ahmad Zaki;L;M. Yusuf;08123456789\n10244;Siti Aminah;P;Rahmat;08112233445\n"
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // Write BOM first to ensure Excel parses UTF-8 correctly
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    outputStream.write(templateContent.toByteArray(Charsets.UTF_8))
                }
                logActivity("Mengunduh Template Excel (.csv) Impor Data Siswa")
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // =========================================================================
    // MODUL BARU: PRESENSI & ABSENSI SISWA PAI (ROOM + SUPABASE)
    // =========================================================================
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun createInitialAttendanceRecords(): List<AttendanceRecord> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return listOf(
            AttendanceRecord(id = "att-seed-1", date = today, classId = 1, className = "Kelas 1-A", studentId = 1, studentName = "Ahmad Zaki", gender = "L", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-2", date = today, classId = 1, className = "Kelas 1-A", studentId = 2, studentName = "Siti Aisyah", gender = "P", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-3", date = today, classId = 1, className = "Kelas 1-A", studentId = 3, studentName = "Muhammad Rizki", gender = "L", status = AttendanceStatus.SAKIT, notes = "Flu dan batuk", subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-4", date = today, classId = 1, className = "Kelas 1-A", studentId = 4, studentName = "Fatimah Azzahra", gender = "P", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-5", date = today, classId = 1, className = "Kelas 1-A", studentId = 5, studentName = "Umar Faruq", gender = "L", status = AttendanceStatus.IZIN, notes = "Kepentingan keluarga", subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-6", date = today, classId = 1, className = "Kelas 1-A", studentId = 6, studentName = "Nurul Hidayah", gender = "P", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-7", date = today, classId = 2, className = "Kelas 4-B", studentId = 7, studentName = "Bilal Habasyi", gender = "L", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-8", date = today, classId = 2, className = "Kelas 4-B", studentId = 8, studentName = "Khadijah Al-Kubro", gender = "P", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-9", date = today, classId = 2, className = "Kelas 4-B", studentId = 9, studentName = "Zaid bin Haritsah", gender = "L", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti"),
            AttendanceRecord(id = "att-seed-10", date = today, classId = 2, className = "Kelas 4-B", studentId = 10, studentName = "Maryam binti Imran", gender = "P", status = AttendanceStatus.HADIR, subject = "PAI & Budi Pekerti")
        )
    }

    fun saveAttendanceBatch(
        date: String,
        classId: Int,
        className: String,
        subject: String,
        entries: List<AttendanceStudentEntry>
    ) {
        if (!requireStaffPermission("menyimpan presensi")) return
        viewModelScope.launch {
            val existingToDelete = allAttendanceRecords.value.filter {
                it.date == date && it.classId == classId && it.subject == subject
            }
            repository.deleteAttendanceBatch(date, classId, subject)
            existingToDelete.forEach {
                syncEngine.enqueueDelete(SupabaseTables.ATTENDANCE_RECORDS, it.id)
            }

            val newRecords = entries.map { entry ->
                AttendanceRecord(
                    id = "att-${date}-${classId}-${entry.studentId}",
                    date = date,
                    classId = classId,
                    className = className,
                    studentId = entry.studentId,
                    studentName = entry.studentName,
                    gender = entry.gender,
                    status = entry.status,
                    notes = entry.notes,
                    subject = subject
                )
            }
            repository.upsertAttendanceAll(newRecords)
            newRecords.forEach { rec ->
                syncEngine.enqueueUpsert(
                    SupabaseTables.ATTENDANCE_RECORDS,
                    rec.id,
                    syncEngine.encodeAttendance(rec, backendGateway.currentUserId)
                )
            }
            logActivity("Menyimpan presensi $className ($subject) tanggal $date - ${entries.size} siswa")
        }
    }

    fun updateSingleAttendanceStatus(recordId: String, newStatus: AttendanceStatus, notes: String = "") {
        if (!requireStaffPermission("mengubah status presensi")) return
        viewModelScope.launch {
            val existing = allAttendanceRecords.value.find { it.id == recordId } ?: return@launch
            val updated = existing.copy(
                status = newStatus,
                notes = if (notes.isNotEmpty()) notes else existing.notes,
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertAttendance(updated)
            syncEngine.enqueueUpsert(
                SupabaseTables.ATTENDANCE_RECORDS,
                updated.id,
                syncEngine.encodeAttendance(updated, backendGateway.currentUserId)
            )
        }
    }

    fun deleteAttendanceBatch(date: String, classId: Int, subject: String) {
        if (!requireStaffPermission("menghapus data presensi")) return
        viewModelScope.launch {
            val existing = allAttendanceRecords.value.filter {
                it.date == date && it.classId == classId && it.subject == subject
            }
            repository.deleteAttendanceBatch(date, classId, subject)
            existing.forEach { syncEngine.enqueueDelete(SupabaseTables.ATTENDANCE_RECORDS, it.id) }
            logActivity("Menghapus data presensi kelas ID $classId tanggal $date")
        }
    }

    fun formatAttendanceShareText(
        date: String,
        className: String,
        subject: String,
        records: List<AttendanceRecord>
    ): String {
        val total = records.size
        val hadir = records.count { it.status == AttendanceStatus.HADIR }
        val sakit = records.count { it.status == AttendanceStatus.SAKIT }
        val izin = records.count { it.status == AttendanceStatus.IZIN }
        val alpa = records.count { it.status == AttendanceStatus.ALPA }
        val pct = if (total > 0) (hadir * 100) / total else 0

        val sb = StringBuilder()
        sb.append("📋 *REKAP PRESENSI HARIAN SISWA PAI*\n")
        sb.append("🏫 ${schoolData.value.schoolName}\n")
        sb.append("📅 Tanggal: $date\n")
        sb.append("📚 Mata Pelajaran: $subject\n")
        sb.append("👥 Rombel: $className\n")
        sb.append("👨‍🏫 Guru Pengampu: ${teacherProfile.value.fullName}\n")
        sb.append("------------------------------------\n")
        sb.append("📊 *Ringkasan Kehadiran:*\n")
        sb.append("• Total Siswa: $total anak\n")
        sb.append("• Hadir: $hadir anak ($pct%)\n")
        sb.append("• Sakit: $sakit anak\n")
        sb.append("• Izin: $izin anak\n")
        sb.append("• Alpa: $alpa anak\n")
        sb.append("------------------------------------\n")
        sb.append("📝 *Daftar Siswa Tidak Hadir:*\n")

        val absentList = records.filter { it.status != AttendanceStatus.HADIR }
        if (absentList.isEmpty()) {
            sb.append("Alhamdulillah seluruh siswa hadir 100%.\n")
        } else {
            absentList.forEachIndexed { idx, rec ->
                val reason = if (rec.notes.isNotBlank()) " (${rec.notes})" else ""
                sb.append("${idx + 1}. ${rec.studentName} [${rec.status.label}]$reason\n")
            }
        }
        sb.append("------------------------------------\n")
        sb.append("Generated by SIM-PAI SDN Kiarapandak 01, Kec. Sukajaya")
        return sb.toString()
    }

    // =========================================================================
    // MODUL BARU: HAFALAN SURAT-SURAT PENDEK (ROOM + SUPABASE)
    // =========================================================================
    val surahCatalog = SurahCatalogProvider.surahs

    val allHafalanRecords: StateFlow<List<HafalanRecord>> = repository.allHafalan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun createInitialHafalanRecords(): List<HafalanRecord> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return listOf(
            HafalanRecord(id = "haf-seed-1", studentId = 1, studentName = "Ahmad Zaki", classId = 1, className = "Kelas 1-A", surahNumber = 1, surahName = "Al-Fatihah", date = today, score = 95, status = HafalanStatus.MUMTAZ, kelancaran = "Sangat Lancar", tajwid = "Sangat Baik", makhraj = "Fasih", notes = "Makhraj huruf 'ain dan ha sangat bersih."),
            HafalanRecord(id = "haf-seed-2", studentId = 1, studentName = "Ahmad Zaki", classId = 1, className = "Kelas 1-A", surahNumber = 114, surahName = "An-Nas", date = today, score = 92, status = HafalanStatus.MUMTAZ, kelancaran = "Sangat Lancar", tajwid = "Sangat Baik", makhraj = "Fasih", notes = "Ghunnah nun bertasydid ditahan 2 harakat."),
            HafalanRecord(id = "haf-seed-3", studentId = 2, studentName = "Siti Aisyah", classId = 1, className = "Kelas 1-A", surahNumber = 1, surahName = "Al-Fatihah", date = today, score = 90, status = HafalanStatus.JAYYID_JIDDAN, kelancaran = "Lancar", tajwid = "Baik", makhraj = "Fasih", notes = "Bagus sekali, terus tingkatkan."),
            HafalanRecord(id = "haf-seed-4", studentId = 2, studentName = "Siti Aisyah", classId = 1, className = "Kelas 1-A", surahNumber = 113, surahName = "Al-Falaq", date = today, score = 88, status = HafalanStatus.JAYYID_JIDDAN, kelancaran = "Lancar", tajwid = "Baik", makhraj = "Fasih", notes = "Qalqalah kubra pada akhir ayat sudah terdengar memantul."),
            HafalanRecord(id = "haf-seed-5", studentId = 3, studentName = "Muhammad Rizki", classId = 1, className = "Kelas 1-A", surahNumber = 112, surahName = "Al-Ikhlas", date = today, score = 75, status = HafalanStatus.PERLU_MURAJAAH, kelancaran = "Cukup Lancar", tajwid = "Cukup", makhraj = "Cukup", notes = "Perlu muraja'ah di rumah pada ayat ke-4."),
            HafalanRecord(id = "haf-seed-6", studentId = 7, studentName = "Bilal Habasyi", classId = 2, className = "Kelas 4-B", surahNumber = 107, surahName = "Al-Ma'un", date = today, score = 96, status = HafalanStatus.MUMTAZ, kelancaran = "Sangat Lancar", tajwid = "Sangat Baik", makhraj = "Fasih", notes = "Tartil dan penghayatan makna sangat baik."),
            HafalanRecord(id = "haf-seed-7", studentId = 7, studentName = "Bilal Habasyi", classId = 2, className = "Kelas 4-B", surahNumber = 105, surahName = "Al-Fil", date = today, score = 94, status = HafalanStatus.MUMTAZ, kelancaran = "Sangat Lancar", tajwid = "Sangat Baik", makhraj = "Fasih", notes = "Bagus dan lancar."),
            HafalanRecord(id = "haf-seed-8", studentId = 8, studentName = "Khadijah Al-Kubro", classId = 2, className = "Kelas 4-B", surahNumber = 103, surahName = "Al-'Asr", date = today, score = 95, status = HafalanStatus.MUMTAZ, kelancaran = "Sangat Lancar", tajwid = "Sangat Baik", makhraj = "Fasih", notes = "Huruf shad dan ra sukun dibaca tebal (tafkhim) dengan tepat.")
        )
    }

    fun addOrUpdateHafalanRecord(record: HafalanRecord) {
        if (!requireStaffPermission("mencatat setoran hafalan")) return
        viewModelScope.launch {
            val existing = allHafalanRecords.value.find {
                it.studentId == record.studentId && it.surahNumber == record.surahNumber
            }
            val finalRecord = (if (existing != null) record.copy(id = existing.id) else record)
                .copy(score = record.score.coerceIn(0, 100), updatedAt = System.currentTimeMillis())
            repository.upsertHafalan(finalRecord)
            syncEngine.enqueueUpsert(
                SupabaseTables.HAFALAN_RECORDS,
                finalRecord.id,
                syncEngine.encodeHafalan(finalRecord, backendGateway.currentUserId)
            )
            logActivity("Mencatat setoran hafalan ${finalRecord.surahName} untuk siswa ${finalRecord.studentName} (Skor: ${finalRecord.score})")
        }
    }

    fun deleteHafalanRecord(id: String) {
        if (!requireStaffPermission("menghapus data hafalan")) return
        viewModelScope.launch {
            repository.deleteHafalanById(id)
            syncEngine.enqueueDelete(SupabaseTables.HAFALAN_RECORDS, id)
            logActivity("Menghapus data hafalan ID: $id")
        }
    }

    // =========================================================================
    // MODUL SUPABASE STORAGE: ARSIP DIGITAL & DOKUMEN CLOUD
    // =========================================================================
    private val _isUploadingArchive = MutableStateFlow(false)
    val isUploadingArchive: StateFlow<Boolean> = _isUploadingArchive.asStateFlow()

    fun uploadDigitalArchiveFromUri(
        uri: Uri,
        title: String,
        category: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!requireStaffPermission("mengunggah berkas arsip")) {
            onResult(false, "Mode Siswa hanya dapat mengunduh/melihat berkas.")
            return
        }
        viewModelScope.launch {
            _isUploadingArchive.value = true
            try {
                val resolver = getApplication<Application>().contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                val fileName = resolveFileName(uri)
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Berkas tidak dapat dibaca.")
                if (bytes.isEmpty()) throw IllegalStateException("Berkas kosong.")

                val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val userPrefix = backendGateway.currentUserId ?: "local"
                val archiveId = "arc-${java.util.UUID.randomUUID()}"
                val storagePath = "$userPrefix/${System.currentTimeMillis()}_$safeName"

                val uploadResult = backendGateway.uploadFile(storagePath, bytes, mimeType)
                if (uploadResult.isFailure) {
                    throw uploadResult.exceptionOrNull() ?: Exception("Gagal unggah ke Supabase Storage")
                }
                val signedOrPath = uploadResult.getOrThrow()
                val sizeLabel = formatFileSize(bytes.size.toLong())
                val item = DigitalArchiveItem(
                    id = archiveId,
                    title = title.trim().ifBlank { fileName },
                    category = category.trim().ifBlank { "Umum" },
                    fileName = fileName,
                    fileSize = sizeLabel,
                    mimeType = mimeType,
                    storagePath = storagePath,
                    publicUrl = signedOrPath,
                    uploadedByName = currentUserProfile.value?.fullName ?: teacherProfile.value.fullName
                )
                repository.upsertDigitalArchive(item)
                syncEngine.enqueueUpsert(
                    SupabaseTables.DIGITAL_ARCHIVES,
                    item.id,
                    syncEngine.encodeDigitalArchive(item, backendGateway.currentUserId)
                )
                logActivity("Mengunggah arsip digital '$fileName' ($sizeLabel) ke Supabase Storage")
                onResult(true, "Berkas '$fileName' berhasil diunggah ke Supabase Storage.")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Gagal mengunggah berkas.")
            } finally {
                _isUploadingArchive.value = false
            }
        }
    }

    fun getArchiveSignedUrl(item: DigitalArchiveItem, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            val res = backendGateway.createDownloadUrl(item.storagePath)
            if (res.isSuccess) {
                onResult(res.getOrThrow(), null)
            } else {
                onResult(null, res.exceptionOrNull()?.localizedMessage ?: "Gagal membuat tautan unduhan")
            }
        }
    }

    fun downloadArchiveToUri(item: DigitalArchiveItem, targetUri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = backendGateway.downloadFileBytes(item.storagePath)
                if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Gagal mengunduh berkas")
                val bytes = res.getOrThrow()
                getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(bytes)
                }
                logActivity("Mengunduh arsip cloud: ${item.fileName}")
                onResult(true, "Berkas '${item.fileName}' berhasil disimpan ke perangkat.")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Gagal mengunduh berkas.")
            }
        }
    }

    fun deleteDigitalArchive(item: DigitalArchiveItem, onResult: (Boolean, String) -> Unit) {
        if (!requireStaffPermission("menghapus arsip digital")) {
            onResult(false, "Mode Siswa tidak dapat menghapus arsip.")
            return
        }
        viewModelScope.launch {
            backendGateway.deleteFile(item.storagePath)
            repository.deleteDigitalArchiveById(item.id)
            syncEngine.enqueueDelete(SupabaseTables.DIGITAL_ARCHIVES, item.id)
            logActivity("Menghapus arsip cloud: ${item.fileName}")
            onResult(true, "Arsip '${item.fileName}' berhasil dihapus.")
        }
    }

    private fun resolveFileName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "dokumen_pai"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "dokumen_pai"
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun formatHafalanShareText(
        studentName: String,
        className: String,
        records: List<HafalanRecord>
    ): String {
        val sb = StringBuilder()
        sb.append("📖 *BUKU MUTABA'AH HAFALAN SURAT PENDEK*\n")
        sb.append("🏫 ${schoolData.value.schoolName}\n")
        sb.append("👤 Nama Siswa: $studentName\n")
        sb.append("👥 Rombel: $className\n")
        sb.append("👨‍🏫 Penguji: ${teacherProfile.value.fullName}\n")
        sb.append("------------------------------------\n")
        sb.append("🏆 *Daftar Capaian Surat Pendek (Juz 'Amma):*\n")

        if (records.isEmpty()) {
            sb.append("Belum ada riwayat setoran hafalan tercatat.\n")
        } else {
            records.forEachIndexed { idx, r ->
                sb.append("${idx + 1}. Q.S. ${r.surahName} [Nilai: ${r.score} - ${r.status.label}]\n")
                sb.append("   • Kelancaran: ${r.kelancaran} | Tajwid: ${r.tajwid}\n")
                if (r.notes.isNotBlank()) sb.append("   • Catatan: ${r.notes}\n")
            }
        }
        sb.append("------------------------------------\n")
        val lulusCount = records.count { it.status != HafalanStatus.PERLU_MURAJAAH && it.status != HafalanStatus.BELUM_SETOR }
        sb.append("✨ Total Surat Tuntas: $lulusCount dari ${records.size} setoran disimak.\n")
        sb.append("Semoga ananda istiqamah menjaga hafalan kalam Allah SWT.\n")
        sb.append("SIM-PAI Kec. Sukajaya")
        return sb.toString()
    }

    // --- Warna Latar Cerah (Bright Theme Modes) ---
    val currentBrightTheme = MutableStateFlow(BrightThemeMode.HIJAU_MINT_CERAH)

    fun selectBrightTheme(theme: BrightThemeMode) {
        currentBrightTheme.value = theme
        logActivity("Mengubah tema background cerah ke: ${theme.title}")
    }
}

enum class BrightThemeMode(
    val title: String,
    val bgHex: Long,
    val subtitle: String
) {
    HIJAU_MINT_CERAH("Hijau Mint Cerah", 0xFFF0FDF4, "Segar, Islami & Nyaman di Mata"),
    PUTIH_BERSIH("Putih Murni Bersih", 0xFFFFFFFF, "Elegan & Terang Maksimal"),
    KUNING_HANGAT("Kuning Mentari Hangat", 0xFFFFFBEB, "Hangat, Luminous & Ceria"),
    BIRU_LANGIT("Biru Langit Sejuk", 0xFFF0F9FF, "Cerah, Tenang & Menyejukkan")
}


