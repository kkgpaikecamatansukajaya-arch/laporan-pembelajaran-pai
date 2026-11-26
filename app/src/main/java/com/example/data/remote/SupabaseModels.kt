package com.example.data.remote

import com.example.data.models.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseProfileDto(
    val id: String,
    val email: String = "",
    @SerialName("full_name") val fullName: String = "",
    val nip: String = "",
    val role: String = AppRole.SISWA.dbValue,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("school_name") val schoolName: String = "SD Negeri Kiarapandak 01",
    @SerialName("class_name") val className: String = "",
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toManagedUser(): ManagedUserProfile = ManagedUserProfile(
        id = id,
        email = email,
        fullName = fullName,
        nip = nip,
        role = role,
        isActive = isActive,
        schoolName = schoolName,
        className = className
    )
}

@Serializable
data class SupabaseSchoolDataDto(
    val id: Long = 1L,
    @SerialName("school_name") val schoolName: String,
    val npsn: String = "",
    val address: String = "",
    @SerialName("headmaster_name") val headmasterName: String = "",
    @SerialName("academic_year") val academicYear: String = "2026/2027",
    val semester: String = "Ganjil",
    val curriculum: String = "Kurikulum Merdeka",
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity() = SchoolData(
        id = 1,
        schoolName = schoolName,
        npsn = npsn,
        address = address,
        headmasterName = headmasterName,
        academicYear = academicYear,
        semester = semester,
        curriculum = curriculum
    )
}

@Serializable
data class SupabaseTeacherProfileDto(
    val id: Long = 1L,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("full_name") val fullName: String,
    val nip: String = "",
    val nuptk: String = "",
    @SerialName("cert_no") val certNo: String = "",
    val rank: String = "",
    val education: String = "",
    val email: String = "",
    val phone: String = "",
    @SerialName("employment_status") val employmentStatus: String = "PPPK",
    @SerialName("subject_taught") val subjectTaught: String = "Pendidikan Agama Islam dan Budi Pekerti",
    @SerialName("work_unit") val workUnit: String = "SD Negeri Kiarapandak 01",
    @SerialName("additional_duties") val additionalDuties: String = "",
    @SerialName("birth_place_date") val birthPlaceDate: String = ""
) {
    fun toEntity() = TeacherProfile(
        id = 1,
        fullName = fullName,
        nip = nip,
        nuptk = nuptk,
        certNo = certNo,
        rank = rank,
        education = education,
        email = email,
        phone = phone,
        employmentStatus = employmentStatus,
        subjectTaught = subjectTaught,
        workUnit = workUnit,
        additionalDuties = additionalDuties,
        birthPlaceDate = birthPlaceDate
    )
}

@Serializable
data class SupabaseClassRoomDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    val name: String,
    val level: String,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseStudentDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    @SerialName("class_id") val classId: Long = 1L,
    @SerialName("class_sync_key") val classSyncKey: String = "",
    val nis: String = "",
    val name: String,
    val gender: String = "L",
    @SerialName("parent_name") val parentName: String = "",
    @SerialName("parent_phone") val parentPhone: String = "",
    val nisn: String = "",
    @SerialName("birth_place") val birthPlace: String = "",
    @SerialName("birth_date") val birthDate: String = "",
    val address: String = "",
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseMerdekaDocumentDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    val type: String,
    val title: String,
    val subject: String = "Pendidikan Agama Islam dan Budi Pekerti",
    @SerialName("class_level") val classLevel: String = "Semua",
    @SerialName("content_text") val contentText: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("file_uri") val fileUri: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: String? = null,
    @SerialName("file_mime_type") val fileMimeType: String? = null,
    @SerialName("storage_path") val storagePath: String? = null,
    val semester: String = "Ganjil",
    @SerialName("academic_year") val academicYear: String = "2026/2027",
    @SerialName("author_teacher") val authorTeacher: String = "",
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseTeachingJournalDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    val date: String,
    @SerialName("class_id") val classId: Long = 1L,
    @SerialName("class_name") val className: String,
    val topic: String,
    val activity: String,
    @SerialName("teacher_present") val teacherPresent: Boolean = true,
    @SerialName("student_presence_json") val studentPresenceJson: String = "",
    @SerialName("location_gps") val locationGps: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseAssessmentRecordDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    @SerialName("student_id") val studentId: Long = 0L,
    @SerialName("student_name") val studentName: String,
    @SerialName("class_id") val classId: Long = 1L,
    val type: String,
    val topic: String,
    val score: Int,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseQuestionDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    val text: String,
    @SerialName("option_a") val optionA: String = "",
    @SerialName("option_b") val optionB: String = "",
    @SerialName("option_c") val optionC: String = "",
    @SerialName("option_d") val optionD: String = "",
    @SerialName("answer_key") val answerKey: String = "A",
    val difficulty: String = "Sedang",
    val type: String = "Pilihan Ganda",
    val explanation: String = "",
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseReligiousActivityDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    val title: String,
    val date: String,
    val description: String = "",
    val type: String,
    @SerialName("participant_count") val participantCount: Int = 0,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseExtracurricularDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    val title: String,
    @SerialName("mentor_name") val mentorName: String,
    val schedule: String,
    @SerialName("participants_count") val participantsCount: Int = 0,
    val description: String = "",
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class SupabaseAttendanceRecordDto(
    val id: String,
    val date: String,
    @SerialName("class_id") val classId: Long,
    @SerialName("class_name") val className: String,
    @SerialName("student_id") val studentId: Long,
    @SerialName("student_name") val studentName: String,
    val gender: String = "L",
    val status: String = "HADIR",
    val notes: String = "",
    val subject: String = "PAI & Budi Pekerti",
    @SerialName("created_by") val createdBy: String? = null
) {
    fun toEntity() = AttendanceRecord(
        id = id,
        date = date,
        classId = classId.toInt(),
        className = className,
        studentId = studentId.toInt(),
        studentName = studentName,
        gender = gender,
        status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.HADIR),
        notes = notes,
        subject = subject
    )
}

@Serializable
data class SupabaseHafalanRecordDto(
    val id: String,
    @SerialName("student_id") val studentId: Long,
    @SerialName("student_name") val studentName: String,
    @SerialName("class_id") val classId: Long,
    @SerialName("class_name") val className: String,
    @SerialName("surah_number") val surahNumber: Int,
    @SerialName("surah_name") val surahName: String,
    val date: String,
    val score: Int = 90,
    val status: String = "MUMTAZ",
    val kelancaran: String = "Sangat Lancar",
    val tajwid: String = "Sangat Baik",
    val makhraj: String = "Fasih",
    @SerialName("tester_teacher") val testerTeacher: String = "",
    val notes: String = "",
    @SerialName("created_by") val createdBy: String? = null
) {
    fun toEntity() = HafalanRecord(
        id = id,
        studentId = studentId.toInt(),
        studentName = studentName,
        classId = classId.toInt(),
        className = className,
        surahNumber = surahNumber,
        surahName = surahName,
        date = date,
        score = score,
        status = runCatching { HafalanStatus.valueOf(status) }.getOrDefault(HafalanStatus.MUMTAZ),
        kelancaran = kelancaran,
        tajwid = tajwid,
        makhraj = makhraj,
        testerTeacher = testerTeacher,
        notes = notes
    )
}

@Serializable
data class SupabaseSyncMessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_nip") val senderNip: String = "",
    @SerialName("sender_name") val senderName: String,
    @SerialName("receiver_nip") val receiverNip: String = "ALL",
    @SerialName("message_text") val messageText: String,
    @SerialName("file_uri") val fileUri: String? = null,
    @SerialName("file_type") val fileType: String = "TEXT",
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val timestamp: Long,
    val status: String = "SENT",
    @SerialName("connection_method") val connectionMethod: String = "INTERNET_CLOUD"
) {
    fun toEntity() = SyncMessage(
        id = id,
        senderNip = senderNip,
        senderName = senderName,
        receiverNip = receiverNip,
        messageText = messageText,
        fileUri = fileUri,
        fileType = runCatching { FileType.valueOf(fileType) }.getOrDefault(FileType.TEXT),
        fileName = fileName,
        fileSize = fileSize,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        durationSeconds = durationSeconds,
        timestamp = timestamp,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
        connectionMethod = runCatching { ConnectionType.valueOf(connectionMethod) }.getOrDefault(ConnectionType.INTERNET_CLOUD)
    )
}

@Serializable
data class SupabaseDigitalArchiveDto(
    val id: String,
    val title: String,
    val category: String = "Umum",
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: String = "",
    @SerialName("mime_type") val mimeType: String = "application/octet-stream",
    @SerialName("storage_path") val storagePath: String,
    @SerialName("public_url") val publicUrl: String = "",
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("uploaded_by_name") val uploadedByName: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity() = DigitalArchiveItem(
        id = id,
        title = title,
        category = category,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        storagePath = storagePath,
        publicUrl = publicUrl,
        uploadedByName = uploadedByName,
        createdAt = createdAt
    )
}

@Serializable
data class SupabaseActivityLogDto(
    val id: Long? = null,
    @SerialName("sync_key") val syncKey: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_email") val userEmail: String = "",
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_type") val userType: String,
    val text: String,
    @SerialName("device_info") val deviceInfo: String = "Android",
    val timestamp: Long
)
