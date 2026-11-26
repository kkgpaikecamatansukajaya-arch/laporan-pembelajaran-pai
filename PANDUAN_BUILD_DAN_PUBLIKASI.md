# Panduan Setup Supabase, Build APK/AAB, dan Publikasi Aplikasi **ADM GURU PAI**

Dokumen ini menjelaskan langkah lengkap menyiapkan backend cloud **Supabase PostgreSQL**, mengonfigurasi kunci proyek dengan aman, mem-build **APK / Android App Bundle (AAB)**, serta mempublikasikan aplikasi **ADM GURU PAI** agar siap digunakan secara multi-perangkat (HP Guru, Kepala Sekolah, Operator KKG, dan Siswa).

---

## 1. Persiapan Backend Supabase PostgreSQL

Proyek ini terhubung ke endpoint Supabase:
- **URL Proyek:** `https://fnztmondnvgyrkuctlty.supabase.co`

### Langkah 1.1 — Jalankan Skrip SQL Instalasi
1. Buka **Supabase Dashboard** proyek Anda → menu **SQL Editor** → **New query**.
2. Salin seluruh isi file `/supabase/supabase_schema.sql` dari repositori ini, lalu klik **Run**.
3. Skrip tersebut akan otomatis membuat dan mengaktifkan:
   - **16 Tabel PostgreSQL:** `profiles`, `school_data`, `teacher_profiles`, `class_rooms`, `students`, `merdeka_documents`, `teaching_journals`, `assessment_records`, `questions`, `religious_activities`, `extracurriculars`, `attendance_records`, `hafalan_records`, `sync_messages`, `digital_archives`, dan `activity_logs`.
   - **Trigger Autentikasi & Keamanan:**
     - `handle_new_user()` otomatis membuat profil pengguna saat registrasi. Email `kkgpaikecamatansukajaya@gmail.com` otomatis mendapatkan peran `super_admin`, sedangkan pendaftar mandiri lainnya otomatis berstatus `siswa` sampai dinaikkan perannya oleh Admin/Super Admin.
     - `prevent_self_privilege_escalation()` mencegah pengguna menaikkan hak aksesnya sendiri.
   - **Row Level Security (RLS):** Diaktifkan pada seluruh tabel agar `Super Admin`, `Admin`, `Guru`, dan `Siswa` hanya mengakses dan mengubah data sesuai hak aksesnya.
   - **Supabase Realtime:** Mengaktifkan `REPLICA IDENTITY FULL` dan publikasi `supabase_realtime` pada seluruh tabel agar perubahan data (tambah, edit, hapus) di HP pertama langsung diterima otomatis oleh HP kedua dan perangkat lainnya.
   - **Supabase Storage:** Membuat bucket privat `pai-documents` beserta kebijakan RLS untuk unggah, unduh, dan hapus berkas sertifikat/SK/perangkat ajar.

### Langkah 1.2 — Verifikasi Authentication & Akun Super Admin Awal
1. Di Supabase Dashboard → **Authentication → Providers → Email**:
   - Pastikan **Enable Email provider** aktif.
   - Untuk onboarding cepat sekolah, Anda dapat menonaktifkan *Confirm email* sementara atau memastikan SMTP aktif agar verifikasi email terkirim.
2. Daftarkan akun Super Admin utama melalui layar **Daftar** di aplikasi menggunakan email `kkgpaikecamatansukajaya@gmail.com`. Trigger database akan langsung menetapkan peran **Super Admin**, sehingga Anda bisa mengelola peran **Admin**, **Guru**, dan **Siswa** dari menu **Dashboard Admin & Akses**.

### Langkah 1.3 — Akses Dashboard dari HP maupun Komputer
- **Melalui HP / Tablet Android:** Buka menu samping (Drawer) → **Dashboard Admin & Akses**. Pada layar tablet/Chromebook (`>= 840dp`), aplikasi otomatis menampilkan *Navigation Rail* dan tata letak multi-kolom adaptif.
- **Melalui Browser Komputer:** Anda dapat langsung mengelola seluruh tabel, pengguna, riwayat aktivitas, dan file Storage melalui **Supabase Studio (Table Editor & Storage)** di browser komputer, serta menjalankan aplikasi Android di **Android Studio / Chromebook / Emulator** dengan tata letak layar lebar (*expanded*).

---

## 2. Konfigurasi Kunci Proyek (Keamanan)

> **PERINGATAN KEAMANAN:** Jangan pernah memasukkan `service_role` key atau Secret Key ke dalam kode Android maupun `.env` aplikasi! Gunakan hanya **Publishable / Anon Key**.

### Jika Menggunakan AI Studio Secrets Panel
1. Buka panel **Secrets** di Google AI Studio.
2. Tambahkan / periksa variabel berikut:
   - `SUPABASE_URL` = `https://fnztmondnvgyrkuctlty.supabase.co`
   - `SUPABASE_PUBLISHABLE_KEY` = *(salin **anon / publishable key** dari Supabase Dashboard → Project Settings → API)*
   - `GEMINI_API_KEY` = *(opsional, untuk Asisten AI & Studio Media)*

### Jika Build Lokal dari Komputer (Android Studio / Terminal)
1. Salin `.env.example` menjadi `.env` di folder root proyek (jangan commit `.env` ke Git!).
2. Isi nilai `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, dan `GEMINI_API_KEY` di file `.env`.
3. Secrets Gradle Plugin akan otomatis menyuntikkannya ke `BuildConfig`.

---

## 3. Cara Build APK & Android App Bundle (AAB)

Pastikan Anda menjalankan perintah Gradle dari direktori root proyek (atau gunakan perintah `gradle` di environment cloud).

### 3.1 Build Debug APK (Untuk Pengujian Internal / Uji Dua Perangkat)
```bash
gradle :app:assembleDebug
```
Output APK tersedia di:
`app/build/outputs/apk/debug/app-debug.apk`

### 3.2 Menjalankan Unit Test & Uji Sinkronisasi Multi-Perangkat
```bash
gradle :app:testDebugUnitTest
```

### 3.3 Build Release APK & AAB (Untuk Produksi / Google Play)
1. Siapkan **Release Keystore** (`.jks` / `.keystore`) menggunakan Android Studio (*Build → Generate Signed Bundle / APK*) atau `keytool`:
   ```bash
   keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias adm_guru_pai
   ```
2. Simpan file keystore di tempat aman (di luar Git) dan konfigurasi penandatanganan rilis melalui Android Studio atau environment CI/CD Anda.
3. Jalankan build Release:
   ```bash
   gradle :app:bundleRelease
   ```
   atau untuk APK distribusi langsung:
   ```bash
   gradle :app:assembleRelease
   ```
   Output rilis berada di:
   - AAB: `app/build/outputs/bundle/release/app-release.aab`
   - APK: `app/build/outputs/apk/release/app-release.apk`

---

## 4. Panduan Publikasi Aplikasi

### Opsi A — Publikasi ke Google Play Store
1. Masuk ke **Google Play Console** → **Create app** (Nama: **ADM GURU PAI**, Bahasa default: Indonesia).
2. Lengkapi halaman **Store listing**: ikon adaptif, minimal 2–4 screenshot HP dan tablet, serta deskripsi singkat/lengkap (tanpa kata promosi berlebihan).
3. Isi **App content**:
   - **Privacy Policy:** cantumkan tautan kebijakan privasi sekolah/KKG yang menjelaskan penggunaan Supabase Auth, PostgreSQL, dan Storage.
   - **Data Safety:** nyatakan pengumpulan email/nama untuk autentikasi serta penyimpanan data pembelajaran & dokumen sekolah terenkripsi HTTPS.
   - **Target Audience & Ads:** sesuaikan kategori pendidikan (tidak menargetkan anak-anak di bawah umur sebagai pemilik akun utama; guru/operator bertindak sebagai pengelola).
4. Pilih track **Internal testing** terlebih dahulu untuk menguji sinkronisasi antar-HP guru KKG PAI, lalu promosikan ke **Production** dengan mengunggah file `app-release.aab`.

### Opsi B — Distribusi Langsung Sekolah / KKG PAI
1. Bagikan file `app-release.apk` (atau `app-debug.apk` untuk uji coba terbatas) melalui grup KKG PAI / tautan arsip cloud resmi.
2. Pada perangkat Android guru, aktifkan izin **Instal aplikasi dari sumber ini** saat memasang APK.
3. Guru cukup membuka aplikasi, membuat akun/login, dan Super Admin (`kkgpaikecamatansukajaya@gmail.com`) menetapkan peran **Guru** atau **Admin** di menu **Dashboard Admin & Akses**.
