package com.example.data.models

enum class SchoolAdminStatus(val label: String) {
    LENGKAP("Lengkap"),
    DALAM_PROSES("Dalam Proses"),
    PERLU_PENDAMPINGAN("Perlu Pendampingan")
}

/**
 * Data Model Resmi Guru PAI Kecamatan Sukajaya
 * Berdasarkan Rekapitulasi Data SIAGA Kemenag & Satminkal Sekolah Induk
 */
data class SukajayaTeacherItem(
    val no: Int,
    val name: String,
    val akunSiaga: String, // "-" jika belum ada
    val nuptk: String, // "-" jika belum ada
    val isCertified: Boolean = true, // Kolom SERTI: YA / -
    val lulusPpg: Boolean = false,
    val nonSerti: Boolean = false,
    val statusKepegawaian: String, // "PPPK 2024", "PPPK 2025", "PPPK 2026", "PNS DEPAG", "PNS DINAS", "HONORER"
    val satminkal: String, // Nama Sekolah Induk (e.g. "SDN SUKAJAYA 03")
    val npsn: String,
    val desa: String,
    val totalStudentsPai: Int = 160,
    val classesHandled: String = "Fase A, B, C (Kls 1-6)",
    val cpAtpDone: Boolean = true,
    val modulAjarDone: Boolean = true,
    val protaPromesDone: Boolean = true,
    val kktpDone: Boolean = true,
    val jurnalDone: Boolean = true,
    val asesmenDone: Boolean = true,
    val ekstraDone: Boolean = true,
    val verifiedByKkg: Boolean = true,
    val lastVerifiedDate: String = "2026-09-20",
    val notes: String = "Administrasi Kurikulum Merdeka CP 020/2026 terverifikasi KKG."
) {
    val progressPercent: Int
        get() {
            var count = 0
            val total = 7
            if (cpAtpDone) count++
            if (modulAjarDone) count++
            if (protaPromesDone) count++
            if (kktpDone) count++
            if (jurnalDone) count++
            if (asesmenDone) count++
            if (ekstraDone) count++
            return (count * 100) / total
        }

    val status: SchoolAdminStatus
        get() = when {
            progressPercent >= 100 -> SchoolAdminStatus.LENGKAP
            progressPercent >= 70 -> SchoolAdminStatus.DALAM_PROSES
            else -> SchoolAdminStatus.PERLU_PENDAMPINGAN
        }
}

/**
 * Data Model Pemantauan Progres Per Satminkal (Sekolah Induk)
 * Mendukung multiple guru PAI dalam satu satminkal
 */
data class SukajayaSchoolProgress(
    val id: Int,
    val schoolName: String,
    val npsn: String,
    val desa: String,
    val teachers: List<SukajayaTeacherItem> = emptyList(),
    val teacherName: String = "", // Nama gabungan/guru utama
    val teacherNip: String = "-",
    val employmentStatus: String = "PPPK", // Status agregat/utama
    val isCertified: Boolean = true,
    val totalStudentsPai: Int = 180,
    val classesHandled: String = "Fase A, B, C (Kls 1-6)",
    val cpAtpDone: Boolean = true,
    val modulAjarDone: Boolean = true,
    val protaPromesDone: Boolean = true,
    val kktpDone: Boolean = true,
    val jurnalDone: Boolean = true,
    val asesmenDone: Boolean = true,
    val ekstraDone: Boolean = true,
    val verifiedByKkg: Boolean = true,
    val lastVerifiedDate: String = "2026-09-20",
    val notes: String = "Administrasi Kurikulum Merdeka CP 020/2026 lengkap."
) {
    val progressPercent: Int
        get() {
            if (teachers.isNotEmpty()) {
                return teachers.map { it.progressPercent }.average().toInt()
            }
            var count = 0
            val total = 7
            if (cpAtpDone) count++
            if (modulAjarDone) count++
            if (protaPromesDone) count++
            if (kktpDone) count++
            if (jurnalDone) count++
            if (asesmenDone) count++
            if (ekstraDone) count++
            return (count * 100) / total
        }

    val status: SchoolAdminStatus
        get() = when {
            progressPercent >= 100 -> SchoolAdminStatus.LENGKAP
            progressPercent >= 70 -> SchoolAdminStatus.DALAM_PROSES
            else -> SchoolAdminStatus.PERLU_PENDAMPINGAN
        }

    val teacherCount: Int
        get() = if (teachers.isNotEmpty()) teachers.size else 1
}

data class SukajayaPaiSummary(
    val totalSchools: Int = 25,
    val totalTeachers: Int = 33,
    val pnsCount: Int = 2,
    val pppkCount: Int = 28,
    val pppk2024Count: Int = 15,
    val pppk2025Count: Int = 1,
    val pppk2026Count: Int = 12,
    val honorerCount: Int = 3,
    val certifiedCount: Int = 31,
    val uncertifiedCount: Int = 2,
    val s1Count: Int = 31,
    val s2Count: Int = 2,
    val completeSchoolsCount: Int = 18,
    val inProgressSchoolsCount: Int = 6,
    val needAssistanceSchoolsCount: Int = 1,
    val averageProgressPercent: Int = 92,
    val kkgChairperson: String = "Muhamad Suhendi",
    val kkgSecretary: String = "Ahmad Rapei",
    val kkgTreasurer: String = "Cicih Mintarsih"
)

object SukajayaDataProvider {

    /**
     * Seluruh 33 Guru PAI resmi di Kecamatan Sukajaya sesuai dokumen data SIAGA & Satminkal
     */
    fun getAllTeachers(): List<SukajayaTeacherItem> = listOf(
        SukajayaTeacherItem(
            no = 1,
            name = "Ahmad Rapei",
            akunSiaga = "320140004849",
            nuptk = "9837744647200082",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN SUKAJAYA 03",
            npsn = "20200814",
            desa = "Sukajaya",
            totalStudentsPai = 168,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Sekretaris KKG PAI Sukajaya. Berkas modul ajar dan CP 020 lengkap 100%."
        ),
        SukajayaTeacherItem(
            no = 2,
            name = "ALFI",
            akunSiaga = "320140004494",
            nuptk = "8140757660200023",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN BANAR 2",
            npsn = "20200810",
            desa = "Harkatjaya",
            totalStudentsPai = 142,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Kelengkapan administrasi dan jurnal mengajar digital tersinkron."
        ),
        SukajayaTeacherItem(
            no = 3,
            name = "ATMAWATI",
            akunSiaga = "320120005433",
            nuptk = "0642762663230262",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN CILEUKSA 01",
            npsn = "20201608",
            desa = "Cileuksa",
            totalStudentsPai = 175,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Administrasi lengkap, pembiasaan shalat dhuha dan tadarus aktif."
        ),
        SukajayaTeacherItem(
            no = 4,
            name = "Azizah oktavitasari",
            akunSiaga = "320170005168",
            nuptk = "2357772673230273",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN CISARUA 02",
            npsn = "20230494",
            desa = "Cisarua",
            totalStudentsPai = 135,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Modul ajar berdiferensiasi dan instrumen asesmen Kurmer tuntas."
        ),
        SukajayaTeacherItem(
            no = 5,
            name = "CICIH MINTARSIH",
            akunSiaga = "320172000660",
            nuptk = "4937747651300022",
            isCertified = true,
            statusKepegawaian = "PNS DEPAG",
            satminkal = "SDN SUKAJAYA 03",
            npsn = "20200814",
            desa = "Sukajaya",
            totalStudentsPai = 155,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Bendahara KKG PAI Kec. Sukajaya. Status PNS Depag, berkas valid 100%."
        ),
        SukajayaTeacherItem(
            no = 6,
            name = "DADAY",
            akunSiaga = "320148005738",
            nuptk = "4749772673130352",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN BOJONG",
            npsn = "20201454",
            desa = "Sipayung",
            totalStudentsPai = 120,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = false,
            ekstraDone = true,
            verifiedByKkg = false,
            lastVerifiedDate = "2026-09-17",
            notes = "Sedang melengkapi bank soal asesmen sumatif tengah semester."
        ),
        SukajayaTeacherItem(
            no = 7,
            name = "DEVI",
            akunSiaga = "-",
            nuptk = "-",
            isCertified = true,
            statusKepegawaian = "HONORER",
            satminkal = "SDN CIPARAHU",
            npsn = "20201725",
            desa = "Sukamulih",
            totalStudentsPai = 110,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Guru Honorer telah bersertifikasi pendidik, berkas administrasi tertib."
        ),
        SukajayaTeacherItem(
            no = 8,
            name = "DEWI KARIMATU DERAJAT",
            akunSiaga = "320125005627",
            nuptk = "4354772673230263",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SD N PASIRMADANG 02",
            npsn = "20200186",
            desa = "Pasir Madang",
            totalStudentsPai = 148,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Perangkat pembelajaran Kurikulum Merdeka Fase B & C lengkap."
        ),
        SukajayaTeacherItem(
            no = 9,
            name = "DIAH SOBARIAH",
            akunSiaga = "320182005491",
            nuptk = "434753656300012",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN BANAR 01",
            npsn = "20200809",
            desa = "Harkatjaya",
            totalStudentsPai = 138,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Berkas CP, ATP, dan modul ajar P5-PPRA telah terverifikasi."
        ),
        SukajayaTeacherItem(
            no = 10,
            name = "Dundin imanudin",
            akunSiaga = "320175004210",
            nuptk = "9853774675130122",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN SIPAYUNG 1",
            npsn = "20200239",
            desa = "Sipayung",
            totalStudentsPai = 162,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Kelengkapan modul ajar dan jurnal harian tuntas 100%."
        ),
        SukajayaTeacherItem(
            no = 11,
            name = "Eet juherti",
            akunSiaga = "320161005104",
            nuptk = "1260753658300003",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN CIPEUNDEUY 2",
            npsn = "20201728",
            desa = "Sukamulih",
            totalStudentsPai = 125,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Administrasi terverifikasi KKG dan siap untuk supervisi pengawas."
        ),
        SukajayaTeacherItem(
            no = 12,
            name = "EMAN SULAIMAN",
            akunSiaga = "320182005626",
            nuptk = "-",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SD N SUKAJAYA 05",
            npsn = "20200816",
            desa = "Sukajaya",
            totalStudentsPai = 130,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Tersinkronisasi dengan akun SIAGA Kemenag."
        ),
        SukajayaTeacherItem(
            no = 13,
            name = "FITRIA MAULAN",
            akunSiaga = "-",
            nuptk = "3753771672230372",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN KIARAPANDAK 01",
            npsn = "20201616",
            desa = "Kiarapandak",
            totalStudentsPai = 95,
            classesHandled = "Fase A (Kls 1-2)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Guru PAI Satminkal SDN Kiarapandak 01 Fase A, modul ajar tuntas."
        ),
        SukajayaTeacherItem(
            no = 14,
            name = "IDA RESDIANA",
            akunSiaga = "320124004188",
            nuptk = "3636765666230252",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN SUKAJAYA 01",
            npsn = "20201598",
            desa = "Sukajaya",
            totalStudentsPai = 160,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Administrasi PAI lengkap, koordinator kegiatan keagamaan sekolah."
        ),
        SukajayaTeacherItem(
            no = 15,
            name = "IMAS SULASTRI",
            akunSiaga = "320181004162",
            nuptk = "7135757659300003",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN CALINCING",
            npsn = "20201474",
            desa = "Urug",
            totalStudentsPai = 115,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Kelengkapan berkas sangat rapi dan tertib jurnal mengajar."
        ),
        SukajayaTeacherItem(
            no = 16,
            name = "ISMA",
            akunSiaga = "320116004470",
            nuptk = "8740764665130282",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN KIARAPANDAK 2",
            npsn = "20201617",
            desa = "Kiarapandak",
            totalStudentsPai = 120,
            classesHandled = "Fase B (Kls 3-4)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Guru PAI Satminkal SDN Kiarapandak 2 Fase B, administrasi tuntas."
        ),
        SukajayaTeacherItem(
            no = 17,
            name = "IYUS RUSWANA",
            akunSiaga = "320116001381",
            nuptk = "3362756659200003",
            isCertified = true,
            statusKepegawaian = "PNS DINAS",
            satminkal = "SDN CIPEUNDEUY 01",
            npsn = "20201727",
            desa = "Sukamulih",
            totalStudentsPai = 150,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "PNS Dinas Pendidikan, berkas Kurikulum Merdeka valid 100%."
        ),
        SukajayaTeacherItem(
            no = 18,
            name = "JUBAEDAH",
            akunSiaga = "320152003449",
            nuptk = "5036771672230113",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN KIARAPANDAK 2",
            npsn = "20201617",
            desa = "Kiarapandak",
            totalStudentsPai = 110,
            classesHandled = "Fase C (Kls 5-6)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Guru PAI Satminkal SDN Kiarapandak 2 Fase C, administrasi lengkap."
        ),
        SukajayaTeacherItem(
            no = 19,
            name = "JUMADI",
            akunSiaga = "320171004478",
            nuptk = "0159763664130193",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN CISARUA 01",
            npsn = "20230492",
            desa = "Cisarua",
            totalStudentsPai = 145,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Berkas perangkat pembelajaran CP 020 lengkap dan terverifikasi."
        ),
        SukajayaTeacherItem(
            no = 20,
            name = "MAMAS MASPUROH",
            akunSiaga = "320171004211",
            nuptk = "2455775676230042",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN KIARAPANDAK 01",
            npsn = "20201616",
            desa = "Kiarapandak",
            totalStudentsPai = 100,
            classesHandled = "Fase B (Kls 3-4)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Guru PAI Satminkal SDN Kiarapandak 01 Fase B, perangkat tuntas."
        ),
        SukajayaTeacherItem(
            no = 21,
            name = "MUHAMAD IRPAN NUDIN",
            akunSiaga = "320130005737",
            nuptk = "-",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN KOMPA",
            npsn = "20201604",
            desa = "Jayaraharja",
            totalStudentsPai = 128,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = false,
            ekstraDone = true,
            verifiedByKkg = false,
            lastVerifiedDate = "2026-09-16",
            notes = "Instrumen asesmen formatif akhir bab 2 sedang disempurnakan."
        ),
        SukajayaTeacherItem(
            no = 22,
            name = "MUHAMAD SUHENDI",
            akunSiaga = "320151005340",
            nuptk = "3148771672130343",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SDN KIARAPANDAK 01",
            npsn = "20201616",
            desa = "Kiarapandak",
            totalStudentsPai = 194,
            classesHandled = "Fase C (Kls 5-6)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Ketua KKG PAI Kec. Sukajaya. Satminkal SDN Kiarapandak 01, berkas 100% lengkap."
        ),
        SukajayaTeacherItem(
            no = 23,
            name = "Oji Madroji",
            akunSiaga = "320157005347",
            nuptk = "3752757659200042",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN CILEUKSA 5",
            npsn = "20230472",
            desa = "Cileuksa",
            totalStudentsPai = 110,
            classesHandled = "Fase A & B (Kls 1-4)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Administrasi Kurikulum Merdeka terunggah dan tervalidasi KKG."
        ),
        SukajayaTeacherItem(
            no = 24,
            name = "SAEPUL MALIK",
            akunSiaga = "320136003144",
            nuptk = "5833763665110042",
            isCertified = true,
            statusKepegawaian = "PPPK 2025",
            satminkal = "SDN CIPATAT",
            npsn = "20201724",
            desa = "Sukamulih",
            totalStudentsPai = 132,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Status PPPK 2025, modul ajar dan program pembiasaan lengkap."
        ),
        SukajayaTeacherItem(
            no = 25,
            name = "Silvia Nurpadilah",
            akunSiaga = "320117005343",
            nuptk = "6734770671230262",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN SUKAJAYA 01",
            npsn = "20201598",
            desa = "Sukajaya",
            totalStudentsPai = 105,
            classesHandled = "Fase A (Kls 1-2)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-20",
            notes = "Guru PAI Satminkal SDN Sukajaya 01, berkas pembelajaran lengkap."
        ),
        SukajayaTeacherItem(
            no = 26,
            name = "SITI MARIAM",
            akunSiaga = "320114005338",
            nuptk = "7944774675230242",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SD N PASIRMADANG 02",
            npsn = "20200186",
            desa = "Pasir Madang",
            totalStudentsPai = 112,
            classesHandled = "Fase A (Kls 1-2)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Guru PAI Satminkal SDN Pasirmadang 02 Fase A, modul ajar tuntas."
        ),
        SukajayaTeacherItem(
            no = 27,
            name = "SRI ANGGRAENI",
            akunSiaga = "320143005339",
            nuptk = "1248768669300003",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN SIPAYUNG 1",
            npsn = "20200239",
            desa = "Sipayung",
            totalStudentsPai = 118,
            classesHandled = "Fase A (Kls 1-2)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Guru PAI Satminkal SDN Sipayung 1, perangkat ajar lengkap."
        ),
        SukajayaTeacherItem(
            no = 28,
            name = "Turmudi",
            akunSiaga = "320126001614",
            nuptk = "3237768669130253",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SDN CILEUKSA 03",
            npsn = "20230470",
            desa = "Cileuksa",
            totalStudentsPai = 140,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Modul ajar dan program semester Kurikulum Merdeka tuntas."
        ),
        SukajayaTeacherItem(
            no = 29,
            name = "UYUN YUNIAR",
            akunSiaga = "320140005655",
            nuptk = "6747771672230232",
            isCertified = true,
            statusKepegawaian = "PPPK 2024",
            satminkal = "SD N CILEUKSA 05",
            npsn = "20230472",
            desa = "Cileuksa",
            totalStudentsPai = 125,
            classesHandled = "Fase C (Kls 5-6)",
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Guru PAI Satminkal SDN Cileuksa 05 Fase C, administrasi lengkap."
        ),
        SukajayaTeacherItem(
            no = 30,
            name = "YUDI",
            akunSiaga = "-",
            nuptk = "-",
            isCertified = true,
            statusKepegawaian = "HONORER",
            satminkal = "SD N SUKAJAYA 04",
            npsn = "20200815",
            desa = "Sukajaya",
            totalStudentsPai = 122,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-18",
            notes = "Guru Honorer telah bersertifikasi pendidik, berkas administrasi tuntas."
        ),
        SukajayaTeacherItem(
            no = 31,
            name = "SANA",
            akunSiaga = "-",
            nuptk = "-",
            isCertified = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SD N PASIRMADANG 03",
            npsn = "20200187",
            desa = "Pasir Madang",
            totalStudentsPai = 115,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = true,
            jurnalDone = true,
            asesmenDone = true,
            ekstraDone = true,
            verifiedByKkg = true,
            lastVerifiedDate = "2026-09-19",
            notes = "Kelengkapan modul ajar dan program pembiasaan keagamaan lengkap."
        ),
        SukajayaTeacherItem(
            no = 32,
            name = "Imron",
            akunSiaga = "-",
            nuptk = "-",
            isCertified = false,
            nonSerti = true,
            statusKepegawaian = "PPPK 2026",
            satminkal = "SD N PASIRMADANG 04",
            npsn = "20200188",
            desa = "Pasir Madang",
            totalStudentsPai = 108,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = true,
            kktpDone = false,
            jurnalDone = true,
            asesmenDone = false,
            ekstraDone = true,
            verifiedByKkg = false,
            lastVerifiedDate = "2026-09-16",
            notes = "Status Non-Sertifikasi (antrean PPG). Sedang menyusun rubrik KKTP dan instrumen asesmen."
        ),
        SukajayaTeacherItem(
            no = 33,
            name = "Guru PAI SDN Ciputih",
            akunSiaga = "-",
            nuptk = "-",
            isCertified = false,
            nonSerti = true,
            statusKepegawaian = "HONORER",
            satminkal = "SDN CIPUTIH",
            npsn = "20201476",
            desa = "Cileuksa",
            totalStudentsPai = 90,
            cpAtpDone = true,
            modulAjarDone = true,
            protaPromesDone = false,
            kktpDone = false,
            jurnalDone = true,
            asesmenDone = false,
            ekstraDone = true,
            verifiedByKkg = false,
            lastVerifiedDate = "2026-09-14",
            notes = "Status Non-Sertifikasi Honorer. Perlu pendampingan pengurus KKG untuk penyusunan Promes & KKTP."
        )
    )

    /**
     * Mengelompokkan data guru resmi berdasarkan Satminkal (Sekolah Induk) masing-masing
     */
    fun getInitialSchools(): List<SukajayaSchoolProgress> {
        val allTeachers = getAllTeachers()

        // Daftar unik Satminkal terurut dengan representasi sekolah
        val satminkalList = listOf(
            "SDN KIARAPANDAK 01",
            "SDN KIARAPANDAK 2",
            "SDN SUKAJAYA 01",
            "SDN SUKAJAYA 03",
            "SD N SUKAJAYA 04",
            "SD N SUKAJAYA 05",
            "SD N PASIRMADANG 02",
            "SD N PASIRMADANG 03",
            "SD N PASIRMADANG 04",
            "SDN CILEUKSA 01",
            "SDN CILEUKSA 03",
            "SDN CILEUKSA 5",
            "SDN CIPUTIH",
            "SDN CISARUA 01",
            "SDN CISARUA 02",
            "SDN BANAR 01",
            "SDN BANAR 2",
            "SDN SIPAYUNG 1",
            "SDN BOJONG",
            "SDN CIPEUNDEUY 01",
            "SDN CIPEUNDEUY 2",
            "SDN CIPARAHU",
            "SDN CIPATAT",
            "SDN CALINCING",
            "SDN KOMPA"
        )

        return satminkalList.mapIndexed { index, satminkalName ->
            val teachersInSatminkal = allTeachers.filter {
                it.satminkal.equals(satminkalName, ignoreCase = true) ||
                (satminkalName == "SDN CILEUKSA 5" && it.satminkal.contains("CILEUKSA 05", ignoreCase = true))
            }

            val primaryTeacher = teachersInSatminkal.firstOrNull()
            val schoolDesa = primaryTeacher?.desa ?: "Sukajaya"
            val schoolNpsn = primaryTeacher?.npsn ?: "20200000"
            val combinedTeacherNames = if (teachersInSatminkal.isNotEmpty()) {
                teachersInSatminkal.joinToString(", ") { it.name }
            } else {
                "Guru PAI"
            }
            val primaryStatus = teachersInSatminkal.firstOrNull()?.statusKepegawaian ?: "PPPK"
            val allCert = teachersInSatminkal.all { it.isCertified }

            // Menghitung status checklist agregat jika ada multiple guru
            val cpDone = teachersInSatminkal.all { it.cpAtpDone }
            val modulDone = teachersInSatminkal.all { it.modulAjarDone }
            val protaDone = teachersInSatminkal.all { it.protaPromesDone }
            val kktpDone = teachersInSatminkal.all { it.kktpDone }
            val jurnalDone = teachersInSatminkal.all { it.jurnalDone }
            val asesmenDone = teachersInSatminkal.all { it.asesmenDone }
            val ekstraDone = teachersInSatminkal.all { it.ekstraDone }
            val isVerified = teachersInSatminkal.all { it.verifiedByKkg }

            val totalSiswa = teachersInSatminkal.sumOf { it.totalStudentsPai }.let { if (it > 0) it else 150 }

            val notesText = if (teachersInSatminkal.size > 1) {
                "Memiliki ${teachersInSatminkal.size} guru PAI di satminkal ini: ${teachersInSatminkal.joinToString { "${it.name} (${it.statusKepegawaian})" }}."
            } else {
                primaryTeacher?.notes ?: "Administrasi terdaftar resmi di KKG PAI Sukajaya."
            }

            SukajayaSchoolProgress(
                id = index + 1,
                schoolName = satminkalName,
                npsn = schoolNpsn,
                desa = schoolDesa,
                teachers = teachersInSatminkal,
                teacherName = combinedTeacherNames,
                teacherNip = primaryTeacher?.nuptk?.takeIf { it != "-" } ?: "-",
                employmentStatus = primaryStatus,
                isCertified = allCert,
                totalStudentsPai = totalSiswa,
                classesHandled = if (teachersInSatminkal.size > 1) "Fase A, B, C (${teachersInSatminkal.size} Guru)" else "Fase A, B, C (Kls 1-6)",
                cpAtpDone = cpDone,
                modulAjarDone = modulDone,
                protaPromesDone = protaDone,
                kktpDone = kktpDone,
                jurnalDone = jurnalDone,
                asesmenDone = asesmenDone,
                ekstraDone = ekstraDone,
                verifiedByKkg = isVerified,
                lastVerifiedDate = primaryTeacher?.lastVerifiedDate ?: "2026-09-20",
                notes = notesText
            )
        }
    }
}
