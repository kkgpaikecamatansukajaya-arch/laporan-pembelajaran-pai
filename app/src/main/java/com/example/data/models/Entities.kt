package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class AppRole(val dbValue: String, val displayName: String) {
    SUPER_ADMIN("super_admin", "Super Admin"),
    ADMIN("admin", "Admin"),
    GURU("guru", "Guru"),
    SISWA("siswa", "Siswa");

    val isSuperAdmin: Boolean get() = this == SUPER_ADMIN
    val isAdminOrAbove: Boolean get() = this == SUPER_ADMIN || this == ADMIN
    val canEditAcademicData: Boolean get() = this == SUPER_ADMIN || this == ADMIN || this == GURU
    val canAccessAdminDashboard: Boolean get() = this == SUPER_ADMIN || this == ADMIN

    companion object {
        fun fromDbValue(value: String?): AppRole {
            return when (value?.trim()?.lowercase()) {
                "super_admin", "super admin" -> SUPER_ADMIN
                "admin", "kepala sekolah" -> ADMIN
                "guru", "guru pai" -> GURU
                else -> SISWA
            }
        }
    }
}

@Entity(tableName = "managed_users")
data class ManagedUserProfile(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val nip: String = "",
    val role: String = AppRole.SISWA.dbValue,
    val isActive: Boolean = true,
    val schoolName: String = "SD Negeri Kiarapandak 01",
    val className: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val appRole: AppRole get() = AppRole.fromDbValue(role)
}

@Entity(tableName = "teacher_profile")
data class TeacherProfile(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Muhamad Suhendi, S.Pd",
    val nip: String = "199308162024211007",
    val nuptk: String = "0",
    val certNo: String = "",
    val rank: String = "Ahli Pertama / IX",
    val education: String = "S1 Pendidikan Agama Islam",
    val photoPath: String? = null,
    val digitalSignature: String? = null,
    val email: String = "kkgpaikecamatansukajaya@gmail.com",
    val phone: String = "",
    val employmentStatus: String = "PPPK", // PNS, PPPK, Honorer / Non-ASN, GTT
    val subjectTaught: String = "Pendidikan Agama Islam dan Budi Pekerti",
    val workUnit: String = "SD Negeri Kiarapandak 01",
    val additionalDuties: String = "Pembina Rohis & Kegiatan Keagamaan",
    val birthPlaceDate: String = "Bogor, 16 Agustus 1993",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "school_data")
data class SchoolData(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "SD Negeri Kiarapandak 01",
    val npsn: String = "20201616",
    val address: String = "Jl. Raya Cipatat No. 12, Sukajaya, Bogor",
    val headmasterName: String = "Dian Nurhayati, S.Pd.MM",
    val academicYear: String = "2026/2027",
    val semester: String = "Ganjil",
    val curriculum: String = "Kurikulum Merdeka",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "class_rooms")
data class ClassRoom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val level: String, // e.g. "I", "II", "III", "IV", "V", "VI"
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val nis: String,
    val name: String,
    val gender: String, // "L" (Laki-laki) or "P" (Perempuan)
    val parentName: String,
    val parentPhone: String,
    val nisn: String = "",
    val birthPlace: String = "",
    val birthDate: String = "",
    val address: String = "",
    val classSyncKey: String = "",
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "merdeka_documents")
data class MerdekaDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CP", "ATP", "Modul Ajar", "Prota", "Promes", "KKTP", "Materi", "Media", "Asesmen", "Refleksi", "LKPD", "Silabus", "RPP", "Dokumen Unggahan"
    val title: String,
    val subject: String = "Pendidikan Agama Islam dan Budi Pekerti",
    val classLevel: String,
    val contentText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val fileMimeType: String? = null,
    val semester: String = "Ganjil",
    val academicYear: String = "2026/2027",
    val authorTeacher: String = "Muhamad Suhendi, S.Pd",
    val storagePath: String? = null,
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teaching_journals")
data class TeachingJournal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val classId: Int,
    val className: String,
    val topic: String,
    val activity: String,
    val teacherPresent: Boolean = true,
    val studentPresenceJson: String, // JSON or formatted text representation of student attendances
    val locationGps: String? = null,
    val photoPath: String? = null,
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assessment_records")
data class AssessmentRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val studentName: String,
    val classId: Int,
    val type: String, // "Diagnostik", "Formatif", "Sumatif", "Projek"
    val topic: String,
    val score: Int,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val answerKey: String, // "A", "B", "C", "D"
    val difficulty: String, // "HOTS", "Sedang", "Mudah"
    val type: String, // "Pilihan Ganda", "Isian", "Essay"
    val explanation: String = "",
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "religious_activities")
data class ReligiousActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // YYYY-MM-DD
    val description: String,
    val type: String,
    val participantCount: Int = 0,
    val photoPath: String? = null,
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "extracurriculars")
data class Extracurricular(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val mentorName: String,
    val schedule: String,
    val participantsCount: Int = 0,
    val description: String,
    val syncKey: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val text: String,
    val userType: String,
    val userEmail: String = "",
    val userName: String = "",
    val deviceInfo: String = "Android",
    val syncKey: String = UUID.randomUUID().toString()
)

@Entity(tableName = "digital_archives")
data class DigitalArchiveItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String = "Umum",
    val fileName: String,
    val fileSize: String = "",
    val mimeType: String = "application/octet-stream",
    val storagePath: String,
    val publicUrl: String = "",
    val uploadedByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_sync_operations")
data class PendingSyncOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val operation: String, // "UPSERT" or "DELETE"
    val recordKey: String,
    val payloadJson: String,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val lastError: String? = null,
    val nextRetryAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
