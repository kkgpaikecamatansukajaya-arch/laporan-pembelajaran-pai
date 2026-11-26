package com.example.data.dao

import androidx.room.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherProfileDao {
    @Query("SELECT * FROM teacher_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<TeacherProfile?>

    @Query("SELECT * FROM teacher_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): TeacherProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: TeacherProfile)

    @Query("DELETE FROM teacher_profile")
    suspend fun clearProfile()
}

@Dao
interface SchoolDataDao {
    @Query("SELECT * FROM school_data WHERE id = 1 LIMIT 1")
    fun getSchoolData(): Flow<SchoolData?>

    @Query("SELECT * FROM school_data WHERE id = 1 LIMIT 1")
    suspend fun getSchoolDataOnce(): SchoolData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSchoolData(schoolData: SchoolData)
}

@Dao
interface ClassRoomDao {
    @Query("SELECT * FROM class_rooms ORDER BY name ASC")
    fun getAllClasses(): Flow<List<ClassRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classRoom: ClassRoom): Long

    @Update
    suspend fun updateClass(classRoom: ClassRoom)

    @Delete
    suspend fun deleteClass(classRoom: ClassRoom)

    @Query("SELECT * FROM class_rooms WHERE id = :id")
    suspend fun getClassById(id: Int): ClassRoom?

    @Query("SELECT * FROM class_rooms WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getClassBySyncKey(syncKey: String): ClassRoom?

    @Query("SELECT * FROM class_rooms WHERE name = :name LIMIT 1")
    suspend fun getClassByName(name: String): ClassRoom?

    @Query("SELECT * FROM class_rooms")
    suspend fun getAllClassesList(): List<ClassRoom>

    @Query("DELETE FROM class_rooms WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAllStudentsList(): List<Student>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    fun getStudentsByClass(classId: Int): Flow<List<Student>>

    @Query("SELECT COUNT(*) FROM students")
    fun getStudentCount(): Flow<Int>

    @Query("SELECT * FROM students WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getStudentBySyncKey(syncKey: String): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("DELETE FROM students WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)

    @Query("DELETE FROM students WHERE classId = :classId")
    suspend fun deleteStudentsByClass(classId: Int)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    @Query("SELECT * FROM students WHERE name LIKE :query OR nis LIKE :query OR nisn LIKE :query OR parentName LIKE :query ORDER BY name ASC")
    fun searchStudents(query: String): Flow<List<Student>>
}

@Dao
interface MerdekaDocumentDao {
    @Query("SELECT * FROM merdeka_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<MerdekaDocument>>

    @Query("SELECT * FROM merdeka_documents ORDER BY createdAt DESC")
    suspend fun getAllDocumentsList(): List<MerdekaDocument>

    @Query("SELECT * FROM merdeka_documents WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getDocumentBySyncKey(syncKey: String): MerdekaDocument?

    @Query("SELECT * FROM merdeka_documents WHERE type = :type ORDER BY createdAt DESC")
    fun getDocumentsByType(type: String): Flow<List<MerdekaDocument>>

    @Query("SELECT * FROM merdeka_documents WHERE classLevel = :classLevel ORDER BY createdAt DESC")
    fun getDocumentsByClassLevel(classLevel: String): Flow<List<MerdekaDocument>>

    @Query("SELECT * FROM merdeka_documents WHERE title LIKE :query OR contentText LIKE :query OR type LIKE :query ORDER BY createdAt DESC")
    fun searchDocuments(query: String): Flow<List<MerdekaDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MerdekaDocument): Long

    @Update
    suspend fun updateDocument(document: MerdekaDocument)

    @Delete
    suspend fun deleteDocument(document: MerdekaDocument)

    @Query("DELETE FROM merdeka_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    @Query("DELETE FROM merdeka_documents WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)

    @Query("DELETE FROM merdeka_documents WHERE type = :type")
    suspend fun deleteDocumentsByType(type: String)

    @Query("DELETE FROM merdeka_documents")
    suspend fun deleteAllDocuments()
}

@Dao
interface TeachingJournalDao {
    @Query("SELECT * FROM teaching_journals ORDER BY date DESC")
    fun getAllJournals(): Flow<List<TeachingJournal>>

    @Query("SELECT * FROM teaching_journals ORDER BY date DESC")
    suspend fun getAllJournalsList(): List<TeachingJournal>

    @Query("SELECT * FROM teaching_journals WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): TeachingJournal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: TeachingJournal): Long

    @Update
    suspend fun updateJournal(journal: TeachingJournal)

    @Delete
    suspend fun deleteJournal(journal: TeachingJournal)

    @Query("DELETE FROM teaching_journals WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)
}

@Dao
interface AssessmentRecordDao {
    @Query("SELECT * FROM assessment_records ORDER BY date DESC")
    fun getAllAssessments(): Flow<List<AssessmentRecord>>

    @Query("SELECT * FROM assessment_records ORDER BY date DESC")
    suspend fun getAllAssessmentsList(): List<AssessmentRecord>

    @Query("SELECT * FROM assessment_records WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): AssessmentRecord?

    @Query("SELECT * FROM assessment_records WHERE classId = :classId ORDER BY date DESC")
    fun getAssessmentsByClass(classId: Int): Flow<List<AssessmentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(record: AssessmentRecord): Long

    @Update
    suspend fun updateAssessment(record: AssessmentRecord)

    @Delete
    suspend fun deleteAssessment(record: AssessmentRecord)

    @Query("DELETE FROM assessment_records WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions ORDER BY id DESC")
    suspend fun getAllQuestionsList(): List<Question>

    @Query("SELECT * FROM questions WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): Question?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question): Long

    @Update
    suspend fun updateQuestion(question: Question)

    @Delete
    suspend fun deleteQuestion(question: Question)

    @Query("DELETE FROM questions WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)
}

@Dao
interface ReligiousActivityDao {
    @Query("SELECT * FROM religious_activities ORDER BY date DESC")
    fun getAllActivities(): Flow<List<ReligiousActivity>>

    @Query("SELECT * FROM religious_activities ORDER BY date DESC")
    suspend fun getAllActivitiesList(): List<ReligiousActivity>

    @Query("SELECT * FROM religious_activities WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): ReligiousActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ReligiousActivity): Long

    @Update
    suspend fun updateActivity(activity: ReligiousActivity)

    @Delete
    suspend fun deleteActivity(activity: ReligiousActivity)

    @Query("DELETE FROM religious_activities WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)
}

@Dao
interface ExtracurricularDao {
    @Query("SELECT * FROM extracurriculars ORDER BY title ASC")
    fun getAllExtracurriculars(): Flow<List<Extracurricular>>

    @Query("SELECT * FROM extracurriculars ORDER BY title ASC")
    suspend fun getAllExtracurricularsList(): List<Extracurricular>

    @Query("SELECT * FROM extracurriculars WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): Extracurricular?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtracurricular(extracurricular: Extracurricular): Long

    @Update
    suspend fun updateExtracurricular(extracurricular: Extracurricular)

    @Delete
    suspend fun deleteExtracurricular(extracurricular: Extracurricular)

    @Query("DELETE FROM extracurriculars WHERE syncKey = :syncKey")
    suspend fun deleteBySyncKey(syncKey: String)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getBySyncKey(syncKey: String): ActivityLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, studentName ASC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    suspend fun getAllAttendanceList(): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<AttendanceRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attendance_records WHERE date = :date AND classId = :classId AND subject = :subject")
    suspend fun deleteBatch(date: String, classId: Int, subject: String)
}

@Dao
interface HafalanDao {
    @Query("SELECT * FROM hafalan_records ORDER BY date DESC, studentName ASC")
    fun getAllHafalan(): Flow<List<HafalanRecord>>

    @Query("SELECT * FROM hafalan_records ORDER BY date DESC")
    suspend fun getAllHafalanList(): List<HafalanRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: HafalanRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<HafalanRecord>)

    @Query("DELETE FROM hafalan_records WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface DigitalArchiveDao {
    @Query("SELECT * FROM digital_archives ORDER BY createdAt DESC")
    fun getAllArchives(): Flow<List<DigitalArchiveItem>>

    @Query("SELECT * FROM digital_archives ORDER BY createdAt DESC")
    suspend fun getAllArchivesList(): List<DigitalArchiveItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DigitalArchiveItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DigitalArchiveItem>)

    @Query("DELETE FROM digital_archives WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ManagedUserDao {
    @Query("SELECT * FROM managed_users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<ManagedUserProfile>>

    @Query("SELECT * FROM managed_users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ManagedUserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: ManagedUserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<ManagedUserProfile>)

    @Query("DELETE FROM managed_users WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SyncMessageDao {
    @Query("SELECT * FROM sync_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<SyncMessage>>

    @Query("SELECT * FROM sync_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<SyncMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: SyncMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<SyncMessage>)

    @Query("DELETE FROM sync_messages WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SyncHistoryLogDao {
    @Query("SELECT * FROM sync_history_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentSyncHistory(): Flow<List<SyncHistoryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SyncHistoryLog)
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync_operations ORDER BY createdAt ASC")
    fun observePending(): Flow<List<PendingSyncOperation>>

    @Query("SELECT COUNT(*) FROM pending_sync_operations")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_sync_operations WHERE nextRetryAt <= :now ORDER BY createdAt ASC")
    suspend fun getDueOperations(now: Long): List<PendingSyncOperation>

    @Query("SELECT * FROM pending_sync_operations ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingSyncOperation>

    @Query("SELECT * FROM pending_sync_operations WHERE tableName = :tableName AND recordKey = :recordKey LIMIT 1")
    suspend fun findExisting(tableName: String, recordKey: String): PendingSyncOperation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(op: PendingSyncOperation): Long

    @Query("DELETE FROM pending_sync_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync_operations")
    suspend fun clearAll()
}
