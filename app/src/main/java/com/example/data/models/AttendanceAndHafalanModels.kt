package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data model untuk pencatatan Presensi / Absensi Siswa PAI
 */
@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val date: String, // Format: YYYY-MM-DD
    val classId: Int,
    val className: String,
    val studentId: Int,
    val studentName: String,
    val gender: String = "L", // L or P
    val status: AttendanceStatus = AttendanceStatus.HADIR,
    val notes: String = "",
    val subject: String = "PAI & Budi Pekerti",
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus(val label: String, val shortLabel: String, val colorHex: Long) {
    HADIR("Hadir", "H", 0xFF10B981),
    SAKIT("Sakit", "S", 0xFFF59E0B),
    IZIN("Izin", "I", 0xFF0284C7),
    ALPA("Alpa", "A", 0xFFEF4444)
}

data class AttendanceSummary(
    val totalStudents: Int,
    val hadir: Int,
    val sakit: Int,
    val izin: Int,
    val alpa: Int
) {
    val hadirPercentage: Int
        get() = if (totalStudents > 0) ((hadir * 100) / totalStudents) else 0
}

data class AttendanceStudentEntry(
    val studentId: Int,
    val studentName: String,
    val gender: String,
    val status: AttendanceStatus,
    val notes: String
)

/**
 * Data model untuk Katalog Surat-Surat Pendek (Juz 'Amma & Surat Utama PAI SD)
 */
data class SurahItem(
    val number: Int,
    val name: String,
    val arabicName: String,
    val versesCount: Int,
    val meaning: String,
    val revelation: String, // "Makkiyah" / "Madaniyah"
    val fase: String, // "Fase A (Kelas 1-2)", "Fase B (Kelas 3-4)", "Fase C (Kelas 5-6)"
    val orderInJuzAmma: Int,
    val arabicSnippet: String,
    val latinSnippet: String,
    val translationSnippet: String,
    val tajwidFocus: String, // Misal: "Qalqalah, Ghunnah, Mad Thabi'i"
    val wisdom: String // Kandungan pokok / hikmah surat
)

/**
 * Data model untuk Catatan Setoran / Mutaba'ah Hafalan Siswa
 */
@Entity(tableName = "hafalan_records")
data class HafalanRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val studentId: Int,
    val studentName: String,
    val classId: Int,
    val className: String,
    val surahNumber: Int,
    val surahName: String,
    val date: String, // YYYY-MM-DD
    val score: Int = 90, // 0 - 100
    val status: HafalanStatus = HafalanStatus.MUMTAZ,
    val kelancaran: String = "Sangat Lancar", // "Sangat Lancar", "Lancar", "Cukup Lancar", "Kurang Lancar"
    val tajwid: String = "Sangat Baik", // "Sangat Baik (Kaidah Lengkap)", "Baik", "Cukup", "Perlu Bimbingan"
    val makhraj: String = "Fasih", // "Fasih & Tepat", "Cukup", "Kurang Tepat"
    val testerTeacher: String = "Muhamad Suhendi, S.Pd",
    val notes: String = "Hafalan lancar dan tartil, tajwid sesuai kaidah.",
    val updatedAt: Long = System.currentTimeMillis()
)

enum class HafalanStatus(val label: String, val badgeColorHex: Long) {
    MUMTAZ("Lulus (Mumtaz - Sempurna)", 0xFF10B981),
    JAYYID_JIDDAN("Lulus (Jayyid Jiddan - Amat Baik)", 0xFF059669),
    JAYYID("Lulus (Jayyid - Baik)", 0xFF0284C7),
    MAQBUL("Lulus (Maqbul - Cukup)", 0xFFF59E0B),
    PERLU_MURAJAAH("Perlu Muraja'ah / Mengulang", 0xFFDC2626),
    BELUM_SETOR("Belum Disimak", 0xFF94A3B8)
}

/**
 * Katalog Resmi Surat Pendek Juz 'Amma & Target Kurikulum Merdeka PAI SD
 */
object SurahCatalogProvider {
    val surahs = listOf(
        SurahItem(
            number = 1,
            name = "Al-Fatihah",
            arabicName = "الفَاتِحَة",
            versesCount = 7,
            meaning = "Pembukaan",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 0,
            arabicSnippet = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ (١) اَلْحَمْدُ لِلّٰهِ رَبِّ الْعٰلَمِيْنَ (٢) الرَّحْمٰنِ الرَّحِيْمِ (٣) مٰلِكِ يَوْمِ الدِّيْنِ (٤) اِيَّاكَ نَعْبُدُ وَاِيَّاكَ نَسْتَعِيْنُ (٥) اِهْدِنَا الصِّرَاطَ الْمُسْتَقِيْمَ (٦) صِرَاطَ الَّذِيْنَ اَنْعَمْتَ عَلَيْهِمْ ەۙ غَيْرِ الْمَغْضُوْبِ عَلَيْهِمْ وَلَا الضَّاۤلِّيْنَ (٧)",
            latinSnippet = "Bismillāhir-raḥmānir-raḥīm. Al-ḥamdu lillāhi rabbil-'ālamīn. Ar-raḥmānir-raḥīm. Māliki yaumid-dīn. Iyyāka na'budu wa iyyāka nasta'īn. Ihdinaṣ-ṣirāṭal-mustaqīm. Ṣirāṭallażīna an'amta 'alaihim gairil-magḍūbi 'alaihim wa laḍ-ḍāllīn.",
            translationSnippet = "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang. Segala puji bagi Allah, Tuhan seluruh alam. Yang Maha Pengasih, Maha Penyayang. Pemilik hari pembalasan. Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami mohon pertolongan. Tunjukilah kami jalan yang lurus. Yaitu jalan orang-orang yang telah Engkau beri nikmat kepadanya; bukan jalan mereka yang dimurkai dan bukan pula jalan mereka yang sesat.",
            tajwidFocus = "Mad Thabi'i, Mad Wajib Muttashil, Alif Lam Qamariyah & Syamsiyah",
            wisdom = "Ummul Qur'an, rukun salat, doa memohon petunjuk jalan yang lurus."
        ),
        SurahItem(
            number = 114,
            name = "An-Nas",
            arabicName = "النَّاس",
            versesCount = 6,
            meaning = "Manusia",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 1,
            arabicSnippet = "قُلْ اَعُوْذُ بِرَبِّ النَّاسِ (١) مَلِكِ النَّاسِ (٢) اِلٰهِ النَّاسِ (٣) مِنْ شَرِّ الْوَسْوَاسِ ەۙ الْخَنَّاسِ (٤) الَّذِيْ يُوَسْوِسُ فِيْ صُدُوْرِ النَّاسِ (٥) مِنَ الْجِنَّةِ وَالنَّاسِ (٦)",
            latinSnippet = "Qul a'ūżu birabbin-nās. Malikin-nās. Ilāhin-nās. Min syarril-waswāsil-khannās. Allażī yuwaswisu fī ṣudūrin-nās. Minal-jinnati wan-nās.",
            translationSnippet = "Katakanlah, 'Aku berlindung kepada Tuhannya manusia, Raja manusia, Sembahan manusia, dari kejahatan (bisikan) setan yang bersembunyi, yang membisikkan (kejahatan) ke dalam dada manusia, dari (golongan) jin dan manusia.'",
            tajwidFocus = "Ghunnah Musyaddadah (Nun bertasydid), Mad 'Aridh Lissukun",
            wisdom = "Memohon perlindungan kepada Allah dari bisikan jahat setan dan manusia."
        ),
        SurahItem(
            number = 113,
            name = "Al-Falaq",
            arabicName = "الفَلَق",
            versesCount = 5,
            meaning = "Waktu Subuh",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 2,
            arabicSnippet = "قُلْ اَعُوْذُ بِرَبِّ الْفَلَقِ (١) مِنْ شَرِّ مَا خَلَقَ (٢) وَمِنْ شَرِّ غَاسِقٍ اِذَا وَقَبَ (٣) وَمِنْ شَرِّ النَّفّٰثٰتِ فِى الْعُقَدِ (٤) وَمِنْ شَرِّ حَاسِدٍ اِذَا حَسَدَ (٥)",
            latinSnippet = "Qul a'ūżu birabbil-falaq. Min syarri mā khalaq. Wa min syarri gāsiqin iżā waqab. Wa min syarrin-naffāṡāti fil-'uqad. Wa min syarri ḥāsidin iżā ḥasad.",
            translationSnippet = "Katakanlah, 'Aku berlindung kepada Tuhan yang menguasai subuh (fajar), dari kejahatan makhluk yang Dia ciptakan, dan dari kejahatan malam apabila telah gelap gulita, dan dari kejahatan (perempuan-perempuan) penyihir yang meniup pada buhul-buhul (talinya), dan dari kejahatan orang yang dengki apabila dia dengki.'",
            tajwidFocus = "Qalqalah Kubra (waqaf pada huruf Qaf, Ba, Dal), Ikhfa Haqiqi",
            wisdom = "Perlindungan dari kegelapan, sihir, dan sifat iri/dengki (hasad)."
        ),
        SurahItem(
            number = 112,
            name = "Al-Ikhlas",
            arabicName = "الإِخْلَاص",
            versesCount = 4,
            meaning = "Kemurnian Keesaan Allah",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 3,
            arabicSnippet = "قُلْ هُوَ اللّٰهُ اَحَدٌ (١) اَللّٰهُ الصَّمَدُ (٢) لَمْ يَلِدْ وَلَمْ يُوْلَدْ (٣) وَلَمْ يَكُنْ لَّهٗ كُفُوًا اَحَدٌ (٤)",
            latinSnippet = "Qul huwallāhu aḥad. Allāhuṣ-ṣamad. Lam yalid wa lam yūlad. Wa lam yakul lahū kufuwan aḥad.",
            translationSnippet = "Katakanlah (Muhammad), 'Dialah Allah, Yang Maha Esa. Allah tempat meminta segala sesuatu. (Allah) tidak beranak dan tidak pula diperanakkan. Dan tidak ada sesuatu yang setara dengan Dia.'",
            tajwidFocus = "Qalqalah Sughra & Kubra (Dal bertanda sukun & waqaf), Idgham Bila Ghunnah",
            wisdom = "Fondasi akidah tauhid murni; menyamai sepertiga Al-Qur'an."
        ),
        SurahItem(
            number = 111,
            name = "Al-Lahab",
            arabicName = "المَسَد / اللَّهَب",
            versesCount = 5,
            meaning = "Gejolak Api / Tali Sabut",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 4,
            arabicSnippet = "تَبَّتْ يَدَاۤ اَبِيْ لَهَبٍ وَّتَبَّ (١) مَاۤ اَغْنٰى عَنْهُ مَالُهٗ وَمَا كَسَبَ (٢) سَيَصْلٰى نَارًا ذَاتَ لَهَبٍ (٣) وَّامْرَاَتُهٗ ەۗ حَمَّالَةَ الْحَطَبِ (٤) فِيْ جِيْدِهَا حَبْلٌ مِّنْ مَّسَدٍ (٥)",
            latinSnippet = "Tabbat yadā abī lahabiw wa tabb. Mā agnā 'an-hu māluhū wa mā kasab. Sayaṣlā nāran żāta lahab. Wamra'atuh, ḥammālatal-ḥaṭab. Fī jīdihā ḥablum mim masad.",
            translationSnippet = "Binasalah kedua tangan Abu Lahab dan benar-benar binasa dia! Tidaklah berguna baginya hartanya dan apa yang dia usahakan. Kelak dia akan masuk ke dalam api yang bergejolak (neraka). Dan (begitu pula) istrinya, pembawa kayu bakar (penyebar fitnah). Di lehernya ada tali dari sabut yang dipintal.",
            tajwidFocus = "Idgham Bighunnah, Qalqalah Kubra dengan tasydid",
            wisdom = "Peringatan bagi penentang dakwah Islam dan bahaya fitnah serta kesombongan harta."
        ),
        SurahItem(
            number = 110,
            name = "An-Nasr",
            arabicName = "النَّصْر",
            versesCount = 3,
            meaning = "Pertolongan",
            revelation = "Madaniyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 5,
            arabicSnippet = "اِذَا جَاۤءَ نَصْرُ اللّٰهِ وَالْفَتْحُ (١) وَرَاَيْتَ النَّاسَ يَدْخُلُوْنَ فِيْ دِيْنِ اللّٰهِ اَفْوَاجًا (٢) فَسَبِّحْ بِحَمْدِ رَبِّكَ وَاسْتَغْفِرْهُۗ اِنَّهٗ كَانَ تَوَّابًا (٣)",
            latinSnippet = "Iżā jā'a naṣrullāhi wal-fatḥ. Wa ra'aitan-nāsa yadkhulūna fī dīnillāhi afwājā. Fa sabbiḥ biḥamdi rabbika wastagfirh, innahū kāna tawwābā.",
            translationSnippet = "Apabila telah datang pertolongan Allah dan kemenangan, dan engkau melihat manusia berbondong-bondong masuk agama Allah, maka bertasbihlah dengan memuji Tuhanmu dan mohonlah ampunan kepada-Nya. Sungguh, Dia Maha Penerima tobat.",
            tajwidFocus = "Mad Wajib Muttashil, Tafkhim Lafadz Jalalah, Mad Iwadh",
            wisdom = "Perintah bertasbih, bersyukur, dan istigfar atas segala kemenangan dan pertolongan Allah."
        ),
        SurahItem(
            number = 109,
            name = "Al-Kafirun",
            arabicName = "الكَافِرُوْن",
            versesCount = 6,
            meaning = "Orang-orang Kafir",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 6,
            arabicSnippet = "قُلْ يٰٓاَيُّهَا الْكٰفِرُوْنَ (١) لَاۤ اَعْبُدُ مَا تَعْبُدُوْنَ (٢) وَلَاۤ اَنْتُمْ عٰبِدُوْنَ مَآ اَعْبُدُ (٣) وَلَاۤ اَنَا۠ عَابِدٌ مَّا عَبَدْتُّمْ (٤) وَلَاۤ اَنْتُمْ عٰبِدُوْنَ مَآ اَعْبُدُ (٥) لَكُمْ دِيْنُكُمْ وَلِيَ دِيْنِ (٦)",
            latinSnippet = "Qul yā ayyuhal-kāfirūn. Lā a'budu mā ta'budūn. Wa lā antum 'ābidūna mā a'bud. Wa lā ana 'ābidum mā 'abattum. Wa lā antum 'ābidūna mā a'bud. Lakum dīnukum wa liya dīn.",
            translationSnippet = "Katakanlah (Muhammad), 'Wahai orang-orang kafir! Aku tidak akan menyembah apa yang kamu sembah. Dan kamu bukan penyembah apa yang aku sembah. Dan aku tidak pernah menjadi penyembah apa yang kamu sembah, dan kamu tidak pernah (pula) menjadi penyembah apa yang aku sembah. Untukmu agamamu, dan untukku agamaku.'",
            tajwidFocus = "Mad Jaiz Munfashil, Idgham Mutajanisain ('abattum), Ikhfa Haqiqi",
            wisdom = "Toleransi beragama yang tegas: tidak mencampuradukkan akidah dan peribadatan (Lakum diinukum waliyadiin)."
        ),
        SurahItem(
            number = 108,
            name = "Al-Kausar",
            arabicName = "الكَوثَر",
            versesCount = 3,
            meaning = "Nikmat yang Banyak",
            revelation = "Makkiyah",
            fase = "Fase A (Kelas 1-2)",
            orderInJuzAmma = 7,
            arabicSnippet = "اِنَّآ اَعْطَيْنٰكَ الْكَوْثَرَ (١) فَصَلِّ لِرَبِّكَ وَانْحَرْ (٢) اِنَّ شَانِئَكَ هُوَ الْاَبْتَرُ (٣)",
            latinSnippet = "Innā a'ṭainākal-kauṡar. Fa ṣalli lirabbika wan-ḥar. Inna syāni'aka huwal-abtar.",
            translationSnippet = "Sungguh, Kami telah memberimu (Muhammad) nikmat yang banyak. Maka laksanakanlah salat karena Tuhanmu, dan berkurbanlah (sebagai ibadah dan mendekatkan diri kepada Allah). Sungguh, orang-orang yang membencimu dialah yang terputus (dari rahmat Allah).",
            tajwidFocus = "Ghunnah Musyaddadah, Mad Jaiz Munfashil, Izhar Halqi (wan-har)",
            wisdom = "Perintah salat dan berkurban sebagai wujud syukur atas limpahan nikmat Allah."
        ),
        SurahItem(
            number = 107,
            name = "Al-Ma'un",
            arabicName = "المَاعُوْن",
            versesCount = 7,
            meaning = "Barang-barang yang Berguna",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 8,
            arabicSnippet = "اَرَءَيْتَ الَّذِيْ يُكَذِّبُ بِالدِّيْنِ (١) فَذٰلِكَ الَّذِيْ يَدُعُّ الْيَتِيْمَ (٢) وَلَا يَحُضُّ عَلٰى طَعَامِ الْمِسْكِيْنِ (٣) فَوَيْلٌ لِّلْمُصَلِّيْنَ (٤) الَّذِيْنَ هُمْ عَنْ صَلَاتِهِمْ سَاهُوْنَ (٥) الَّذِيْنَ هُمْ يُرَاۤءُوْنَ (٦) وَيَمْنَعُوْنَ الْمَاعُوْنَ (٧)",
            latinSnippet = "Ara'aitallażī yukażżibu bid-dīn. Fa żālikallażī yadu''ul-yatīm. Wa lā yaḥuḍḍu 'alā ṭa'āmil-miskīn. Fa wailul lil-muṣallīn. Allażīna hum 'an ṣalātihim sāhūn. Allażīna hum yurā'ūn. Wa yamna'ūnal-mā'ūn.",
            translationSnippet = "Tahukah kamu (orang) yang mendustakan agama? Maka itulah orang yang menghardik anak yatim, dan tidak mendorong memberi makan orang miskin. Maka celakalah orang yang salat, (yaitu) orang-orang yang lalai terhadap salatnya, yang berbuat ria, dan enggan (memberikan) bantuan barang berguna.",
            tajwidFocus = "Idgham Bila Ghunnah, Mad Wajib Muttashil, Mad 'Aridh Lissukun",
            wisdom = "Kepedulian sosial terhadap anak yatim dan fakir miskin; bahaya riya dan kelalaian dalam salat."
        ),
        SurahItem(
            number = 106,
            name = "Quraisy",
            arabicName = "قُرَيْش",
            versesCount = 4,
            meaning = "Suku Quraisy",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 9,
            arabicSnippet = "لِاِيْلٰفِ قُرَيْشٍ (١) اٖلٰفِهِمْ رِحْلَةَ الشِّتَاۤءِ وَالصَّيْفِ (٢) فَلْيَعْبُدُوْا رَبَّ هٰذَا الْبَيْتِ (٣) الَّذِيْٓ اَطْعَمَهُمْ مِّنْ جُوْعٍ وَّاٰمَنَهُمْ مِّنْ خَوْفٍ (٤)",
            latinSnippet = "Li'īlāfi quraīsy. Īlāfihim riḥlatasy-syitā'i waṣ-ṣaīf. Falya'budū rabba hāżal-baīt. Allażī aṭ'amahum min jū'iw wa āmanahum min khauf.",
            translationSnippet = "Karena kebiasaan orang-orang Quraisy, (yaitu) kebiasaan mereka bepergian pada musim dingin dan musim panas. Maka hendaklah mereka menyembah Tuhan (pemilik) rumah ini (Ka'bah), yang telah memberi makanan kepada mereka untuk menghilangkan lapar dan mengamankan mereka dari rasa ketakutan.",
            tajwidFocus = "Mad Layyin (Quraisy, As-Shaif, Al-Bait, Khauf), Idgham Mimi & Idgham Bighunnah",
            wisdom = "Mensyukuri nikmat keamanan, rezeki, dan memurnikan ibadah kepada Allah SWT."
        ),
        SurahItem(
            number = 105,
            name = "Al-Fil",
            arabicName = "الفِيْل",
            versesCount = 5,
            meaning = "Gajah",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 10,
            arabicSnippet = "اَلَمْ تَرَ كَيْفَ فَعَلَ رَبُّكَ بِاَصْحٰبِ الْفِيْلِ (١) اَلَمْ يَجْعَلْ كَيْدَهُمْ فِيْ تَضْلِيْلٍ (٢) وَّاَرْسَلَ عَلَيْهِمْ طَيْرًا اَبَابِيْلَ (٣) تَرْمِيْهِمْ بِحِجَارَةٍ مِّنْ سِجِّيْلٍ (٤) فَجَعَلَهُمْ كَعَصْفٍ مَّأْكُوْلٍ (٥)",
            latinSnippet = "Alam tara kaifa fa'ala rabbuka bi'aṣ-ḥābil-fīl. Alam yaj'al kaidahum fī taḍlīl. Wa arsala 'alaihim ṭairan abābīl. Tarmīhim biḥijāratim min sijjīl. Fa ja'alahum ka'aṣfim ma'kūl.",
            translationSnippet = "Tidakkah engkau (Muhammad) perhatikan bagaimana Tuhanmu telah bertindak terhadap pasukan bergajah? Bukankah Dia telah menjadikan tipu daya mereka itu sia-sia? Dan Dia mengirimkan kepada mereka burung yang berbondong-bondong, yang melempari mereka dengan batu dari tanah liat yang dibakar, sehingga Dia menjadikan mereka seperti daun-daun yang dimakan (ulat).",
            tajwidFocus = "Izhar Syafawi, Ikhfa Syafawi (tarmihim bihijarah), Qalqalah Sughra",
            wisdom = "Kekuasaan Allah dalam menjaga kesucian Ka'bah dan menghancurkan kesombongan Abrahah."
        ),
        SurahItem(
            number = 104,
            name = "Al-Humazah",
            arabicName = "الهُمَزَة",
            versesCount = 9,
            meaning = "Pengumpat",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 11,
            arabicSnippet = "وَيْلٌ لِّكُلِّ هُمَزَةٍ لُّمَزَةٍ (١) الَّذِيْ جَمَعَ مَالًا وَّعَدَّدَهٗ (٢) يَحْسَبُ اَنَّ مَالَهٗٓ اَخْلَدَهٗ (٣) كَلَّا لَيُنْۢبَذَنَّ فِى الْحُطَمَةِ (٤) وَمَآ اَدْرٰىكَ مَا الْحُطَمَةُ (٥) نَارُ اللّٰهِ الْمُوْقَدَةُ (٦) الَّتِيْ تَطَّلِعُ عَلَى الْاَفْـِٕدَةِ (٧) اِنَّهَا عَلَيْهِمْ مُّؤْصَدَةٌ (٨) فِيْ عَمَدٍ مُّمَدَّدَةٍ (٩)",
            latinSnippet = "Wailul likulli humazatil-lumazah. Allażī jama'a mālaw wa 'addadah. Yaḥsabu anna mālahū akhladah. Kallā layumbażanna fil-ḥuṭamah. Wa mā adrāka mal-ḥuṭamah. Nārullāhil-mūqadah. Allatī taṭṭali'u 'alal-af'idah. Innahā 'alaihim mu'ṣadah. Fī 'amadim mumaddadah.",
            translationSnippet = "Celakalah bagi setiap pengumpat dan pencela, yang mengumpulkan harta dan menghitung-hitungnya. Dia (manusia) mengira bahwa hartanya itu dapat mengekalkannya. Sekali-kali tidak! Pasti dia akan dilemparkan ke dalam (neraka) Hutamah. Dan tahukah kamu apakah Hutamah itu? (Yaitu) api (azab) Allah yang dinyalakan, yang (membakar) sampai ke hati.",
            tajwidFocus = "Iqlab (layumbazanna), Idgham Bila Ghunnah, Mad Shilah Kubra",
            wisdom = "Larangan mencela, mengumpat, serta bahaya ketamakan terhadap harta dunia."
        ),
        SurahItem(
            number = 103,
            name = "Al-'Asr",
            arabicName = "العَصْر",
            versesCount = 3,
            meaning = "Masa / Waktu",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 12,
            arabicSnippet = "وَالْعَصْرِ (١) اِنَّ الْاِنْسَانَ لَفِيْ خُسْرٍ (٢) اِلَّا الَّذِيْنَ اٰمَنُوْا وَعَمِلُوا الصّٰلِحٰتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ (٣)",
            latinSnippet = "Wal-'aṣr. Innal-insāna lafī khusr. Illallażīna āmanū wa 'amiluṣ-ṣāliḥāti wa tawāṣau bil-ḥaqqi wa tawāṣau biṣ-ṣabr.",
            translationSnippet = "Demi masa, sungguh manusia berada dalam kerugian, kecuali orang-orang yang beriman dan mengerjakan kebajikan serta saling menasihati untuk kebenaran dan saling menasihati untuk kesabaran.",
            tajwidFocus = "Tafkhim & Tarqiq Ra, Ikhfa Haqiqi, Qalqalah Kubra",
            wisdom = "Manajemen waktu, amal saleh, dan pentingnya dakwah saling menasihati dalam kesabaran."
        ),
        SurahItem(
            number = 102,
            name = "At-Takasur",
            arabicName = "التَّكَاثُر",
            versesCount = 8,
            meaning = "Bermegah-megahan",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 13,
            arabicSnippet = "اَلْهٰىكُمُ التَّكَاثُرُ (١) حَتّٰى زُرْتُمُ الْمَقَابِرَ (٢) كَلَّا سَوْفَ تَعْلَمُوْنَ (٣) ثُمَّ كَلَّا سَوْفَ تَعْلَمُوْنَ (٤) كَلَّا لَوْ تَعْلَمُوْنَ عِلْمَ الْيَقِيْنِ (٥) لَتَرَوُنَّ الْجَحِيْمَ (٦) ثُمَّ لَتَرَوُنَّهَا عَيْنَ الْيَقِيْنِ (٧) ثُمَّ لَتُسْـَٔلُنَّ يَوْمَىِٕذٍ عَنِ النَّعِيْمِ (٨)",
            latinSnippet = "Alhākumut-takāṡur. Ḥattā zurtumul-maqābir. Kallā saufa ta'lamūn. Ṡumma kallā saufa ta'lamūn. Kallā lau ta'lamūna 'ilmal-yaqīn. Latarawunnal-jaḥīm. Ṡumma latarawunnahā 'ainal-yaqīn. Ṡumma latus'alunna yauma'iżin 'anin-na'īm.",
            translationSnippet = "Bermegah-megahan telah melalaikan kamu, sampai kamu masuk ke dalam kubur. Sekali-kali tidak! Kelak kamu akan mengetahui (akibat perbuatanmu itu). Sungguh, kamu pasti akan ditanya pada hari itu tentang kenikmatan (yang megah di dunia itu).",
            tajwidFocus = "Ghunnah Musyaddadah pada Mim dan Nun bertasydid",
            wisdom = "Peringatan agar tidak terlena oleh gemerlap dunia dan kesiapan menghadapi hari pembalasan."
        ),
        SurahItem(
            number = 101,
            name = "Al-Qari'ah",
            arabicName = "القَارِعَة",
            versesCount = 11,
            meaning = "Hari Kiamat yang Mengguncang",
            revelation = "Makkiyah",
            fase = "Fase B (Kelas 3-4)",
            orderInJuzAmma = 14,
            arabicSnippet = "اَلْقَارِعَةُ (١) مَا الْقَارِعَةُ (٢) وَمَآ اَدْرٰىكَ مَا الْقَارِعَةُ (٣) يَوْمَ يَكُوْنُ النَّاسُ كَالْفَرَاشِ الْمَبْثُوْثِ (٤) وَتَكُوْنُ الْجِبَالُ كَالْعِهْنِ الْمَنْفُوْشِ (٥) فَاَمَّا مَنْ ثَقُلَتْ مَوَازِينُهٗ (٦) فَهُوَ فِيْ عِيْشَةٍ رَّاضِيَةٍ (٧) وَاَمَّا مَنْ خَفَّتْ مَوَازِيْنُهٗ (٨) فَاُمُّهٗ هَاوِيَةٌ (٩) وَمَآ اَدْرٰىكَ مَا هِيَهْ (١٠) نَارٌ حَامِيَةٌ (١١)",
            latinSnippet = "Al-qāri'ah. Mal-qāri'ah. Wa mā adrāka mal-qāri'ah. Yauma yakūnun-nāsu kal-farāsyil-mabṡūṡ. Wa takūnul-jibālu kal-'ihnil-manfūsy. Fa ammā man ṡaqulat mawāzīnuh. Fa huwa fī 'īsyatir rāḍiyah. Wa ammā man khaffat mawāzīnuh. Fa ummuhū hāwiyah. Wa mā adrāka mā hiyah. Nārun ḥāmiyah.",
            translationSnippet = "Hari Kiamat, apakah hari Kiamat itu? Dan tahukah kamu apakah hari Kiamat itu? Pada hari itu manusia seperti laron yang beterbangan, dan gunung-gunung seperti bulu yang dihambur-hamburkan. Maka adapun orang yang berat timbangan (kebaikan)nya, maka dia berada dalam kehidupan yang memuaskan. Dan adapun orang yang ringan timbangan kebaikannya, maka tempat kembalinya adalah neraka Hawiyah.",
            tajwidFocus = "Al-Qamariyah, Ikhfa Haqiqi, Idgham Bila Ghunnah, Izhar Halqi",
            wisdom = "Dahsyatnya hari kiamat dan timbangan amal perbuatan (Mizan)."
        ),
        SurahItem(
            number = 99,
            name = "Az-Zalzalah",
            arabicName = "الزَّلْزَلَة",
            versesCount = 8,
            meaning = "Kegoncangan Dahsyat",
            revelation = "Madaniyah",
            fase = "Fase C (Kelas 5-6)",
            orderInJuzAmma = 16,
            arabicSnippet = "اِذَا زُلْزِلَتِ الْاَرْضُ زِلْزَالَهَا (١) وَاَخْرَجَتِ الْاَرْضُ اَثْقَالَهَا (٢) وَقَالَ الْاِنْسَانُ مَا لَهَا (٣) يَوْمَىِٕذٍ تُحَدِّثُ اَخْبَارَهَا (٤) بِاَنَّ رَبَّكَ اَوْحٰى لَهَا (٥) يَوْمَىِٕذٍ يَّصْدُرُ النَّاسُ اَشْتَاتًا ەۙ لِّيُرَوْا اَعْمَالَهُمْ (٦) فَمَنْ يَّعْمَلْ مِثْقَالَ ذَرَّةٍ خَيْرًا يَّرَهٗ (٧) وَمَنْ يَّعْمَلْ مِثْقَالَ ذَرَّةٍ شَرًّا يَّرَهٗ (٨)",
            latinSnippet = "Iżā zulzilatil-arḍu zilzālahā. Wa akhrajatil-arḍu aṡqālahā. Wa qālal-insānu mā lahā. Yauma'iżin tuḥaddiṡu akhbārahā. Bi'anna rabbaka auḥā lahā. Yauma'iżiy yaṣdurun-nāsu asytātal liyurau a'mālahum. Fa may ya'mal miṡqāla żarratin khairay yarah. Wa may ya'mal miṡqāla żarratin syarray yarah.",
            translationSnippet = "Apabila bumi diguncangkan dengan guncangan yang dahsyat, dan bumi telah mengeluarkan beban-beban berat (yang dikandung)nya, dan manusia bertanya, 'Apa yang terjadi pada bumi ini?' Maka barangsiapa mengerjakan kebaikan seberat zarrah, niscaya dia akan melihat (balasan)nya. Dan barangsiapa mengerjakan kejahatan seberat zarrah, niscaya dia akan melihat (balasan)nya pula.",
            tajwidFocus = "Idgham Bighunnah, Ikhfa Haqiqi, Mad Thabi'i",
            wisdom = "Keadilan mutlak Allah: sekecil apa pun amal kebaikan atau keburukan pasti menuai balasan."
        ),
        SurahItem(
            number = 97,
            name = "Al-Qadr",
            arabicName = "القَدْر",
            versesCount = 5,
            meaning = "Kemuliaan",
            revelation = "Makkiyah",
            fase = "Fase C (Kelas 5-6)",
            orderInJuzAmma = 18,
            arabicSnippet = "اِنَّآ اَنْزَلْنٰهُ فِيْ لَيْلَةِ الْقَدْرِ (١) وَمَآ اَدْرٰىكَ مَا لَيْلَةُ الْقَدْرِ (٢) لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ اَلْفِ شَهْرٍ (٣) تَنَزَّلُ الْمَلٰۤىِٕكَةُ وَالرُّوْحُ فِيْهَا بِاِذْنِ رَبِّهِمْ مِنْ كُلِّ اَمْرٍ (٤) سَلٰمٌ هِيَ حَتّٰى مَطْلَعِ الْفَجْرِ (٥)",
            latinSnippet = "Innā anzalnāhu fī lailatil-qadr. Wa mā adrāka mā lailatul-qadr. Lailatul-qadri khairum min alfi syahr. Tanazzalul-malā'ikatu war-rūḥu fīhā bi'iżni rabbihim min kulli amr. Salāmun hiya ḥattā maṭla'il-fajr.",
            translationSnippet = "Sesungguhnya Kami telah menurunkannya (Al-Qur'an) pada malam qadar. Dan tahukah kamu apakah malam kemuliaan itu? Malam kemuliaan itu lebih baik daripada seribu bulan. Pada malam itu turun para malaikat dan Ruh (Jibril) dengan izin Tuhannya untuk mengatur segala urusan. Sejahteralah (malam itu) sampai terbit fajar.",
            tajwidFocus = "Mad Wajib Muttashil, Qalqalah Sughra, Ghunnah",
            wisdom = "Keagungan malam Lailatul Qadar dan turunnya Al-Qur'an pertama kali ke Baitul Izzah."
        ),
        SurahItem(
            number = 95,
            name = "At-Tin",
            arabicName = "التِّيْن",
            versesCount = 8,
            meaning = "Buah Tin",
            revelation = "Makkiyah",
            fase = "Fase C (Kelas 5-6)",
            orderInJuzAmma = 20,
            arabicSnippet = "وَالتِّيْنِ وَالزَّيْتُوْنِ (١) وَطُوْرِ سِيْنِيْنَ (٢) وَهٰذَا الْبَلَدِ الْاَمِيْنِ (٣) لَقَدْ خَلَقْنَا الْاِنْسَانَ فِيْٓ اَحْسَنِ تَقْوِيْمٍ (٤) ثُمَّ رَدَدْنٰهُ اَسْفَلَ سَافِلِيْنَ (٥) اِلَّا الَّذِيْنَ اٰمَنُوْا وَعَمِلُوا الصّٰلِحٰتِ فَلَهُمْ اَجْرٌ غَيْرُ مَمْنُوْنٍ (٦) فَمَا يُكَذِّبُكَ بَعْدُ بِالدِّيْنِ (٧) اَلَيْسَ اللّٰهُ بِاَحْكَمِ الْحٰكِمِيْنَ (٨)",
            latinSnippet = "Wat-tīni waz-zaitūn. Wa ṭūri sīnīn. Wa hāżal-baladil-amīn. Laqad khalaqnal-insāna fī aḥsani taqwīm. Ṡumma radadnāhu asfala sāfilīn. Illallażīna āmanū wa 'amiluṣ-ṣāliḥāti falahum ajrun gairu mamnūn. Fa mā yukażżibuka ba'du bid-dīn. Alaisallāhu bi'aḥkamil-ḥākimīn.",
            translationSnippet = "Demi (buah) Tin dan (buah) Zaitun, demi gunung Sinai, dan demi negeri (Makkah) yang aman ini. Sungguh, Kami telah menciptakan manusia dalam bentuk yang sebaik-baiknya, kemudian Kami kembalikan dia ke tempat yang serendah-rendahnya, kecuali orang-orang yang beriman dan mengerjakan kebajikan; maka mereka akan mendapat pahala yang tidak putus-putus.",
            tajwidFocus = "Mad Thabi'i, Qalqalah Sughra, Mad 'Aridh Lissukun",
            wisdom = "Penciptaan manusia dalam bentuk fisik dan potensi terbaik, serta kewajiban menjaga iman agar tidak jatuh."
        ),
        SurahItem(
            number = 94,
            name = "Asy-Syarh / Al-Insyirah",
            arabicName = "الشَّرْح",
            versesCount = 8,
            meaning = "Kelapangan Dada",
            revelation = "Makkiyah",
            fase = "Fase C (Kelas 5-6)",
            orderInJuzAmma = 21,
            arabicSnippet = "اَلَمْ نَشْرَحْ لَكَ صَدْرَكَ (١) وَوَضَعْنَا عَنْكَ وِزْرَكَ (٢) الَّذِيْٓ اَنْقَضَ ظَهْرَكَ (٣) وَرَفَعْنَا لَكَ ذِكْرَكَ (٤) فَاِنَّ مَعَ الْعُسْرِ يُسْرًا (٥) اِنَّ مَعَ الْعُسْرِ يُسْرًا (٦) فَاِذَا فَرَغْتَ فَانْصَبْ (٧) وَاِلٰى رَبِّكَ فَارْغَبْ (٨)",
            latinSnippet = "Alam nasyraḥ laka ṣadrak. Wa waḍa'nā 'anka wizrak. Allażī anqaḍa ẓahrak. Wa rafa'nā laka żikrak. Fa inna ma'al-'usri yusrā. Inna ma'al-'usri yusrā. Fa iżā faragta fanṣab. Wa ilā rabbika fargab.",
            translationSnippet = "Bukankah Kami telah melapangkan dadamu (Muhammad)? dan Kami pun telah menurunkan bebanmu darimu, yang memberatkan punggungmu, dan Kami tinggikan sebutan (nama)mu bagimu. Maka sesungguhnya beserta kesulitan ada kemudahan, sesungguhnya beserta kesulitan ada kemudahan. Maka apabila engkau telah selesai (dari suatu urusan), tetaplah bekerja keras (untuk urusan yang lain), dan hanya kepada Tuhanmulah engkau berharap.",
            tajwidFocus = "Ikhfa Haqiqi, Izhar Syafawi, Qalqalah Kubra",
            wisdom = "Optimisme hidup: setiap satu kesulitan selalu diiringi dua kemudahan; etos kerja dan istiqamah berdoa."
        ),
        SurahItem(
            number = 93,
            name = "Ad-Duha",
            arabicName = "الضُّحَى",
            versesCount = 11,
            meaning = "Waktu Dhuha",
            revelation = "Makkiyah",
            fase = "Fase C (Kelas 5-6)",
            orderInJuzAmma = 22,
            arabicSnippet = "وَالضُّحٰى (١) وَالَّيْلِ اِذَا سَجٰى (٢) مَا وَدَّعَكَ رَبُّكَ وَمَا قَلٰى (٣) وَلَلْاٰخِرَةُ خَيْرٌ لَّكَ مِنَ الْاُوْلٰى (٤) وَلَسَوْفَ يُعْطِيْكَ رَبُّكَ فَتَرْضٰى (٥) اَلَمْ يَجِدْكَ يَتِيْمًا فَاٰوٰى (٦) وَوَجَدَكَ ضَاۤلًّا فَهَدٰى (٧) وَوَجَدَكَ عَاۤىِٕلًا فَاَغْنٰى (٨) فَاَمَّا الْيَتِيْمَ فَلَا تَقْهَرْ (٩) وَاَمَّا السَّاۤىِٕلَ فَلَا تَنْهَرْ (١٠) وَاَمَّا بِنِعْمَةِ رَبِّكَ فَحَدِّثْ (١١)",
            latinSnippet = "Waḍ-ḍuḥā. Wal-laili iżā sajā. Mā wadda'aka rabbuka wa mā qalā. Wa lal-ākhiratu khairul laka minal-ūlā. Wa lasaufa yu'ṭīka rabbuka fatarḍā. Alam yajidka yatīman fa āwā. Wa wajadaka ḍāllan fa hadā. Wa wajadaka 'ā'ilan fa agnā. Fa ammal-yatīma falā taqhar. Wa ammas-sā'ila falā tanhar. Wa ammā bini'mati rabbika faḥaddiṡ.",
            translationSnippet = "Demi waktu duha (ketika matahari naik sepenggalah), dan demi malam apabila telah sunyi, Tuhanmu tidak meninggalkan engkau (Muhammad) dan tidak (pula) membencimu. Dan sungguh, yang kemudian itu lebih baik bagimu daripada yang permulaan. Dan sungguh, kelak Tuhanmu pasti memberikan karunia-Nya kepadamu, sehingga engkau menjadi puas. Maka terhadap anak yatim janganlah engkau berlaku sewenang-wenang. Dan terhadap orang yang meminta-minta janganlah engkau menghardik(nya). Dan terhadap nikmat Tuhanmu, hendaklah engkau nyatakan (dengan bersyukur).",
            tajwidFocus = "Mad Lazim Mutsaqqal Kalimi (ḍāllan), Mad Wajib Muttashil, Izhar Halqi (tanhar)",
            wisdom = "Kasih sayang Allah yang tak pernah putus kepada hamba-Nya; anjuran menyantuni anak yatim dan bersyukur atas nikmat."
        )
    )
}
