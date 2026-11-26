package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TeacherProfile::class,
        SchoolData::class,
        ClassRoom::class,
        Student::class,
        MerdekaDocument::class,
        TeachingJournal::class,
        AssessmentRecord::class,
        Question::class,
        ReligiousActivity::class,
        Extracurricular::class,
        ActivityLog::class,
        AttendanceRecord::class,
        HafalanRecord::class,
        DigitalArchiveItem::class,
        ManagedUserProfile::class,
        SyncMessage::class,
        SyncHistoryLog::class,
        PendingSyncOperation::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun teacherProfileDao(): TeacherProfileDao
    abstract fun schoolDataDao(): SchoolDataDao
    abstract fun classRoomDao(): ClassRoomDao
    abstract fun studentDao(): StudentDao
    abstract fun merdekaDocumentDao(): MerdekaDocumentDao
    abstract fun teachingJournalDao(): TeachingJournalDao
    abstract fun assessmentRecordDao(): AssessmentRecordDao
    abstract fun questionDao(): QuestionDao
    abstract fun religiousActivityDao(): ReligiousActivityDao
    abstract fun extracurricularDao(): ExtracurricularDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun hafalanDao(): HafalanDao
    abstract fun digitalArchiveDao(): DigitalArchiveDao
    abstract fun managedUserDao(): ManagedUserDao
    abstract fun syncMessageDao(): SyncMessageDao
    abstract fun syncHistoryLogDao(): SyncHistoryLogDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "sim_pai_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(DatabaseCallback(scope, appContext))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope,
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // 1. Populate Teacher Profile & School Data with defaults
            db.teacherProfileDao().insertOrUpdateProfile(TeacherProfile())
            db.schoolDataDao().insertOrUpdateSchoolData(
                SchoolData(
                    schoolName = "SD Negeri Kiarapandak 01",
                    npsn = "20201616",
                    address = "Jl. Raya Cipatat KM 12 RT 04, Kiarapandak, Kec. Sukajaya, Kab. Bogor",
                    headmasterName = "Dian Nurhayati, S.Pd.MM",
                    academicYear = "2026/2027",
                    semester = "Ganjil"
                )
            )

            // 2. Populate Classrooms & Students from Dapodik JSON asset
            try {
                val jsonString = context.assets.open("students_dapodik.json").bufferedReader().use { it.readText() }
                val rootObj = org.json.JSONObject(jsonString)
                val classesArray = rootObj.getJSONArray("classes")
                val classIdMap = mutableMapOf<String, Int>()
                for (i in 0 until classesArray.length()) {
                    val cObj = classesArray.getJSONObject(i)
                    val name = cObj.getString("name")
                    val level = cObj.getString("level")
                    val id = db.classRoomDao().insertClass(ClassRoom(name = name, level = level))
                    classIdMap[name] = id.toInt()
                }

                val studentsArray = rootObj.getJSONArray("students")
                val studentList = mutableListOf<Student>()
                for (i in 0 until studentsArray.length()) {
                    val sObj = studentsArray.getJSONObject(i)
                    val cName = sObj.getString("className")
                    val classId = classIdMap[cName] ?: 1
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
                db.studentDao().insertStudents(studentList)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback basic classes
                val classId1 = db.classRoomDao().insertClass(ClassRoom(name = "Kelas 1-A", level = "I"))
                val classId2 = db.classRoomDao().insertClass(ClassRoom(name = "Kelas 4-A", level = "IV"))
                val classId3 = db.classRoomDao().insertClass(ClassRoom(name = "Kelas 5-A", level = "V"))
            }

            // 4. Populate Questions (Bank Soal)
            db.questionDao().insertQuestion(Question(
                text = "Di bawah ini yang merupakan rukun Islam yang pertama adalah...",
                optionA = "Membayar Zakat",
                optionB = "Mengucapkan Dua Kalimat Syahadat",
                optionC = "Mendirikan Salat",
                optionD = "Melaksanakan Ibadah Haji",
                answerKey = "B",
                difficulty = "Mudah",
                type = "Pilihan Ganda",
                explanation = "Rukun Islam pertama adalah mengucap dua kalimat syahadat."
            ))
            db.questionDao().insertQuestion(Question(
                text = "Surah Al-Fatihah disebut juga sebagai Ummul Kitab, artinya...",
                optionA = "Induk Al-Qur'an",
                optionB = "Penutup Al-Qur'an",
                optionC = "Pembuka Rezeki",
                optionD = "Hiasan Mushaf",
                answerKey = "A",
                difficulty = "Sedang",
                type = "Pilihan Ganda",
                explanation = "Ummul Kitab berarti Induk Al-Qur'an karena mencakup seluruh inti ajaran Al-Qur'an."
            ))
            db.questionDao().insertQuestion(Question(
                text = "Bagaimana analisis Anda mengenai relevansi konsep toleransi (tasamuh) dalam kehidupan bermasyarakat yang majemuk berdasarkan nilai-nilai Surah Al-Kafirun?",
                optionA = "Mengikuti seluruh ritual ibadah umat agama lain demi menjaga toleransi",
                optionB = "Menghormati perbedaan keyakinan dan tidak mencampuradukkan aqidah/ibadah",
                optionC = "Memaksa orang lain untuk mengikuti agama kita secara perlahan",
                optionD = "Menjauhi interaksi sosial dengan umat non-Muslim",
                answerKey = "B",
                difficulty = "HOTS",
                type = "Pilihan Ganda",
                explanation = "Surah Al-Kafirun mengajarkan tasamuh dalam bidang sosial kemasyarakatan dengan tetap menjaga keteguhan aqidah masing-masing ('Untukmu agamamu, dan untukku agamaku')."
            ))

            // 5. Populate Extracurriculars
            db.extracurricularDao().insertExtracurricular(Extracurricular(title = "Rohis (Rohani Islam)", mentorName = "Muhamad Suhendi, S.Pd", schedule = "Jumat, 13:30 - 15:00", participantsCount = 25, description = "Kegiatan pendalaman keagamaan, mentoring, dan pengembangan kepribadian muslim."))
            db.extracurricularDao().insertExtracurricular(Extracurricular(title = "Tahfidz Al-Qur'an", mentorName = "Ust. Muhammad Hafiz", schedule = "Selasa & Kamis, 15:30 - 16:30", participantsCount = 18, description = "Bimbingan menghafal Al-Qur'an Juz 30 dan juz-juz pilihan."))

            // 6. Populate Merdeka Documents (Berdasarkan SK Kepala BSKAP No. 020 Tahun 2026)
            db.merdekaDocumentDao().insertDocument(MerdekaDocument(
                type = "CP",
                title = "Capaian Pembelajaran PAI & BP SD (SK BSKAP No. 020/2026)",
                classLevel = "Semua",
                contentText = """
                    CAPAIAN PEMBELAJARAN PENDIDIKAN AGAMA ISLAM DAN BUDI PEKERTI (PAI-BP)
                    Berdasarkan Keputusan Kepala BSKAP Nomor 020 Tahun 2026
                    (Perubahan atas Keputusan Nomor 046/H/KR/2025)
                    
                    I. RASIONALITAS & DIMENSI CINTA
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
                    Penilaian dirancang berjenjang dari unistruktural, multistruktural, relasional, hingga extended abstract untuk memastikan kedalaman nalar kritis dan penghayatan ibadah murid.
                """.trimIndent()
            ))
            db.merdekaDocumentDao().insertDocument(MerdekaDocument(
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
            ))
            db.merdekaDocumentDao().insertDocument(MerdekaDocument(
                type = "KKTP",
                title = "KKTP PAI Taksonomi SOLO Surah Al-Hujurat:13 (CP 020/2026)",
                classLevel = "IV",
                contentText = """
                    KRITERIA KETERCAPAIAN TUJUAN PEMBELAJARAN (KKTP)
                    Mata Pelajaran: PAI dan Budi Pekerti (Fase B Kelas IV)
                    Acuan: Taksonomi SOLO & SK BSKAP No. 020 Tahun 2026
                    
                    Rubrik Interval SOLO:
                    - Level 1 (Unistruktural / 0-60%): Murid hanya mampu membaca potongan ayat tanpa tajwid yang tepat.
                    - Level 2 (Multistruktural / 61-75%): Murid mampu membaca keseluruhan ayat dengan lancar namun belum memahami terjemah kata.
                    - Level 3 (Relasional / 76-88% - Kategori Tuntas): Murid mampu membaca tartil, memahami terjemahan, dan mengaitkan konsep keragaman dengan sikap rukun di sekolah.
                    - Level 4 (Extended Abstract / 89-100% - Pengayaan): Murid mampu menganalisis solusi atas kasus intoleransi di lingkungan sekitar berlandaskan nilai-nilai Q.S. Al-Hujurat:13.
                """.trimIndent()
            ))

            // 7. Populate Logs
            db.activityLogDao().insertLog(ActivityLog(text = "Sistem pertama kali diinisialisasi dengan data default Kurikulum Merdeka", userType = "Admin"))
        }
    }
}
