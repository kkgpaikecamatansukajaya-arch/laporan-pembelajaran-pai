package com.example.data.remote

import com.example.data.models.AppRole
import com.example.data.models.ManagedUserProfile
import kotlinx.coroutines.flow.Flow

object SupabaseTables {
    const val PROFILES = "profiles"
    const val SCHOOL_DATA = "school_data"
    const val TEACHER_PROFILES = "teacher_profiles"
    const val CLASS_ROOMS = "class_rooms"
    const val STUDENTS = "students"
    const val MERDEKA_DOCUMENTS = "merdeka_documents"
    const val TEACHING_JOURNALS = "teaching_journals"
    const val ASSESSMENT_RECORDS = "assessment_records"
    const val QUESTIONS = "questions"
    const val RELIGIOUS_ACTIVITIES = "religious_activities"
    const val EXTRACURRICULARS = "extracurriculars"
    const val ATTENDANCE_RECORDS = "attendance_records"
    const val HAFALAN_RECORDS = "hafalan_records"
    const val SYNC_MESSAGES = "sync_messages"
    const val DIGITAL_ARCHIVES = "digital_archives"
    const val ACTIVITY_LOGS = "activity_logs"

    val realtimeSyncedTables = listOf(
        PROFILES,
        SCHOOL_DATA,
        TEACHER_PROFILES,
        CLASS_ROOMS,
        STUDENTS,
        MERDEKA_DOCUMENTS,
        TEACHING_JOURNALS,
        ASSESSMENT_RECORDS,
        QUESTIONS,
        RELIGIOUS_ACTIVITIES,
        EXTRACURRICULARS,
        ATTENDANCE_RECORDS,
        HAFALAN_RECORDS,
        SYNC_MESSAGES,
        DIGITAL_ARCHIVES,
        ACTIVITY_LOGS
    )
}

data class RealtimeTableEvent(
    val table: String,
    val actionType: String, // "INSERT", "UPDATE", "DELETE"
    val recordKey: String? = null,
    val payloadJson: String? = null
)

sealed interface SignUpOutcome {
    data class Authenticated(val profile: ManagedUserProfile) : SignUpOutcome
    data class EmailConfirmationRequired(val email: String) : SignUpOutcome
}

data class RemoteDatabaseSnapshot(
    val profiles: List<SupabaseProfileDto> = emptyList(),
    val schoolData: SupabaseSchoolDataDto? = null,
    val teacherProfile: SupabaseTeacherProfileDto? = null,
    val classRooms: List<SupabaseClassRoomDto> = emptyList(),
    val students: List<SupabaseStudentDto> = emptyList(),
    val merdekaDocuments: List<SupabaseMerdekaDocumentDto> = emptyList(),
    val teachingJournals: List<SupabaseTeachingJournalDto> = emptyList(),
    val assessmentRecords: List<SupabaseAssessmentRecordDto> = emptyList(),
    val questions: List<SupabaseQuestionDto> = emptyList(),
    val religiousActivities: List<SupabaseReligiousActivityDto> = emptyList(),
    val extracurriculars: List<SupabaseExtracurricularDto> = emptyList(),
    val attendanceRecords: List<SupabaseAttendanceRecordDto> = emptyList(),
    val hafalanRecords: List<SupabaseHafalanRecordDto> = emptyList(),
    val syncMessages: List<SupabaseSyncMessageDto> = emptyList(),
    val digitalArchives: List<SupabaseDigitalArchiveDto> = emptyList(),
    val activityLogs: List<SupabaseActivityLogDto> = emptyList()
)

interface RemoteBackendGateway {
    val isConfigured: Boolean
    val currentUserId: String?
    val realtimeEvents: Flow<RealtimeTableEvent>

    suspend fun restoreSession(): Result<ManagedUserProfile?>
    suspend fun signIn(email: String, password: String): Result<ManagedUserProfile>
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        nip: String,
        schoolName: String
    ): Result<SignUpOutcome>
    suspend fun signOut(): Result<Unit>

    suspend fun fetchAllProfiles(): Result<List<ManagedUserProfile>>
    suspend fun updateUserRoleAndStatus(userId: String, role: AppRole, isActive: Boolean): Result<Unit>

    suspend fun pullSnapshot(): Result<RemoteDatabaseSnapshot>
    suspend fun upsertRecord(table: String, payloadJson: String): Result<Unit>
    suspend fun deleteRecord(table: String, recordKey: String): Result<Unit>

    suspend fun startRealtimeSync()
    suspend fun stopRealtimeSync()

    suspend fun uploadFile(storagePath: String, bytes: ByteArray, contentType: String): Result<String>
    suspend fun createDownloadUrl(storagePath: String): Result<String>
    suspend fun downloadFileBytes(storagePath: String): Result<ByteArray>
    suspend fun deleteFile(storagePath: String): Result<Unit>
}
