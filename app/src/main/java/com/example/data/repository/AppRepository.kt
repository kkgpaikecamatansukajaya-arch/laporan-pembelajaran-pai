package com.example.data.repository

import com.example.data.dao.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val teacherProfileDao: TeacherProfileDao,
    private val schoolDataDao: SchoolDataDao,
    private val classRoomDao: ClassRoomDao,
    private val studentDao: StudentDao,
    private val merdekaDocumentDao: MerdekaDocumentDao,
    private val teachingJournalDao: TeachingJournalDao,
    private val assessmentRecordDao: AssessmentRecordDao,
    private val questionDao: QuestionDao,
    private val religiousActivityDao: ReligiousActivityDao,
    private val extracurricularDao: ExtracurricularDao,
    private val activityLogDao: ActivityLogDao,
    private val attendanceDao: AttendanceDao,
    private val hafalanDao: HafalanDao,
    private val digitalArchiveDao: DigitalArchiveDao,
    private val managedUserDao: ManagedUserDao
) {
    val profile: Flow<TeacherProfile?> = teacherProfileDao.getProfile()
    val schoolData: Flow<SchoolData?> = schoolDataDao.getSchoolData()
    val allClasses: Flow<List<ClassRoom>> = classRoomDao.getAllClasses()
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val studentCount: Flow<Int> = studentDao.getStudentCount()
    val allDocuments: Flow<List<MerdekaDocument>> = merdekaDocumentDao.getAllDocuments()
    val allJournals: Flow<List<TeachingJournal>> = teachingJournalDao.getAllJournals()
    val allAssessments: Flow<List<AssessmentRecord>> = assessmentRecordDao.getAllAssessments()
    val allQuestions: Flow<List<Question>> = questionDao.getAllQuestions()
    val allActivities: Flow<List<ReligiousActivity>> = religiousActivityDao.getAllActivities()
    val allExtracurriculars: Flow<List<Extracurricular>> = extracurricularDao.getAllExtracurriculars()
    val recentLogs: Flow<List<ActivityLog>> = activityLogDao.getRecentLogs()
    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendance()
    val allHafalan: Flow<List<HafalanRecord>> = hafalanDao.getAllHafalan()
    val allArchives: Flow<List<DigitalArchiveItem>> = digitalArchiveDao.getAllArchives()
    val managedUsers: Flow<List<ManagedUserProfile>> = managedUserDao.getAllUsers()

    suspend fun insertOrUpdateProfile(profile: TeacherProfile) = teacherProfileDao.insertOrUpdateProfile(profile)
    suspend fun clearProfile() = teacherProfileDao.clearProfile()
    suspend fun insertOrUpdateSchoolData(schoolData: SchoolData) = schoolDataDao.insertOrUpdateSchoolData(schoolData)

    suspend fun insertClass(classRoom: ClassRoom): Long = classRoomDao.insertClass(classRoom)
    suspend fun updateClass(classRoom: ClassRoom) = classRoomDao.updateClass(classRoom)
    suspend fun deleteClass(classRoom: ClassRoom) = classRoomDao.deleteClass(classRoom)
    suspend fun getClassById(id: Int): ClassRoom? = classRoomDao.getClassById(id)
    suspend fun getClassByName(name: String): ClassRoom? = classRoomDao.getClassByName(name)
    suspend fun getAllClassesList(): List<ClassRoom> = classRoomDao.getAllClassesList()

    fun getStudentsByClass(classId: Int): Flow<List<Student>> = studentDao.getStudentsByClass(classId)
    suspend fun insertStudent(student: Student): Long = studentDao.insertStudent(student)
    suspend fun insertStudents(students: List<Student>) = studentDao.insertStudents(students)
    suspend fun updateStudent(student: Student) = studentDao.updateStudent(student)
    suspend fun deleteStudent(student: Student) = studentDao.deleteStudent(student)
    suspend fun deleteStudentsByClass(classId: Int) = studentDao.deleteStudentsByClass(classId)
    suspend fun deleteAllStudents() = studentDao.deleteAllStudents()
    fun searchStudents(query: String): Flow<List<Student>> = studentDao.searchStudents("%$query%")

    fun getDocumentsByType(type: String): Flow<List<MerdekaDocument>> = merdekaDocumentDao.getDocumentsByType(type)
    fun getDocumentsByClassLevel(classLevel: String): Flow<List<MerdekaDocument>> = merdekaDocumentDao.getDocumentsByClassLevel(classLevel)
    fun searchDocuments(query: String): Flow<List<MerdekaDocument>> = merdekaDocumentDao.searchDocuments("%$query%")
    suspend fun insertDocument(document: MerdekaDocument): Long = merdekaDocumentDao.insertDocument(document)
    suspend fun updateDocument(document: MerdekaDocument) = merdekaDocumentDao.updateDocument(document)
    suspend fun deleteDocument(document: MerdekaDocument) = merdekaDocumentDao.deleteDocument(document)
    suspend fun deleteDocumentById(id: Int) = merdekaDocumentDao.deleteDocumentById(id)
    suspend fun deleteDocumentsByType(type: String) = merdekaDocumentDao.deleteDocumentsByType(type)
    suspend fun deleteAllDocuments() = merdekaDocumentDao.deleteAllDocuments()

    suspend fun insertJournal(journal: TeachingJournal): Long = teachingJournalDao.insertJournal(journal)
    suspend fun updateJournal(journal: TeachingJournal) = teachingJournalDao.updateJournal(journal)
    suspend fun deleteJournal(journal: TeachingJournal) = teachingJournalDao.deleteJournal(journal)

    fun getAssessmentsByClass(classId: Int): Flow<List<AssessmentRecord>> = assessmentRecordDao.getAssessmentsByClass(classId)
    suspend fun insertAssessment(record: AssessmentRecord): Long = assessmentRecordDao.insertAssessment(record)
    suspend fun updateAssessment(record: AssessmentRecord) = assessmentRecordDao.updateAssessment(record)
    suspend fun deleteAssessment(record: AssessmentRecord) = assessmentRecordDao.deleteAssessment(record)

    suspend fun insertQuestion(question: Question): Long = questionDao.insertQuestion(question)
    suspend fun updateQuestion(question: Question) = questionDao.updateQuestion(question)
    suspend fun deleteQuestion(question: Question) = questionDao.deleteQuestion(question)

    suspend fun insertActivity(activity: ReligiousActivity): Long = religiousActivityDao.insertActivity(activity)
    suspend fun updateActivity(activity: ReligiousActivity) = religiousActivityDao.updateActivity(activity)
    suspend fun deleteActivity(activity: ReligiousActivity) = religiousActivityDao.deleteActivity(activity)

    suspend fun insertExtracurricular(extracurricular: Extracurricular): Long = extracurricularDao.insertExtracurricular(extracurricular)
    suspend fun updateExtracurricular(extracurricular: Extracurricular) = extracurricularDao.updateExtracurricular(extracurricular)
    suspend fun deleteExtracurricular(extracurricular: Extracurricular) = extracurricularDao.deleteExtracurricular(extracurricular)

    suspend fun insertLog(log: ActivityLog) = activityLogDao.insertLog(log)

    suspend fun upsertAttendance(record: AttendanceRecord) = attendanceDao.upsert(record)
    suspend fun upsertAttendanceAll(records: List<AttendanceRecord>) = attendanceDao.upsertAll(records)
    suspend fun deleteAttendanceById(id: String) = attendanceDao.deleteById(id)
    suspend fun deleteAttendanceBatch(date: String, classId: Int, subject: String) =
        attendanceDao.deleteBatch(date, classId, subject)
    suspend fun getAllAttendanceList(): List<AttendanceRecord> = attendanceDao.getAllAttendanceList()

    suspend fun upsertHafalan(record: HafalanRecord) = hafalanDao.upsert(record)
    suspend fun upsertHafalanAll(records: List<HafalanRecord>) = hafalanDao.upsertAll(records)
    suspend fun deleteHafalanById(id: String) = hafalanDao.deleteById(id)
    suspend fun getAllHafalanList(): List<HafalanRecord> = hafalanDao.getAllHafalanList()

    suspend fun upsertDigitalArchive(item: DigitalArchiveItem) = digitalArchiveDao.upsert(item)
    suspend fun deleteDigitalArchiveById(id: String) = digitalArchiveDao.deleteById(id)

    suspend fun upsertManagedUser(user: ManagedUserProfile) = managedUserDao.upsert(user)
    suspend fun upsertManagedUsers(users: List<ManagedUserProfile>) = managedUserDao.upsertAll(users)
}
