-- ============================================================================
-- SUPABASE POSTGRESQL SCHEMA, RLS, REALTIME & STORAGE INSTALLATION SCRIPT
-- Aplikasi: ADM GURU PAI (KKG PAI Kecamatan Sukajaya)
-- Project URL: https://fnztmondnvgyrkuctlty.supabase.co
-- Jalankan script ini di Supabase Dashboard -> SQL Editor
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. TABEL PROFIL & HAK AKSES PENGGUNA (RBAC: super_admin, admin, guru, siswa)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL DEFAULT 'Pengguna PAI',
    nip TEXT NOT NULL DEFAULT '',
    role TEXT NOT NULL DEFAULT 'siswa'
        CHECK (role IN ('super_admin', 'admin', 'guru', 'siswa')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    school_name TEXT NOT NULL DEFAULT 'SD Negeri Kiarapandak 01',
    class_name TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Fungsi helper keamanan (SECURITY DEFINER agar bebas infinite recursion di RLS)
CREATE OR REPLACE FUNCTION public.current_user_role()
RETURNS TEXT
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT role
    FROM public.profiles
    WHERE id = auth.uid() AND is_active = TRUE
    LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.is_active_user()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND is_active = TRUE
    );
$$;

CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_user_role() IN ('super_admin', 'admin', 'guru'), FALSE);
$$;

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_user_role() IN ('super_admin', 'admin'), FALSE);
$$;

CREATE OR REPLACE FUNCTION public.is_super_admin()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_user_role() = 'super_admin', FALSE);
$$;

-- Trigger otomatis update timestamp
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- Trigger otomatis membuat profil saat user mendaftar via Supabase Auth
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    assigned_role TEXT := 'siswa';
    meta_name TEXT;
    meta_nip TEXT;
    meta_school TEXT;
BEGIN
    -- Bootstrap Super Admin untuk akun resmi KKG PAI Kecamatan Sukajaya
    IF LOWER(COALESCE(NEW.email, '')) = 'kkgpaikecamatansukajaya@gmail.com' THEN
        assigned_role := 'super_admin';
    ELSE
        -- Semua pendaftaran mandiri default ke 'siswa' (hak terendah) demi keamanan.
        -- Super Admin / Admin dapat menaikkan hak akses menjadi 'guru' / 'admin'.
        assigned_role := 'siswa';
    END IF;

    meta_name := COALESCE(NULLIF(TRIM(NEW.raw_user_meta_data->>'full_name'), ''), SPLIT_PART(COALESCE(NEW.email, 'Pengguna'), '@', 1));
    meta_nip := COALESCE(TRIM(NEW.raw_user_meta_data->>'nip'), '');
    meta_school := COALESCE(NULLIF(TRIM(NEW.raw_user_meta_data->>'school_name'), ''), 'SD Negeri Kiarapandak 01');

    INSERT INTO public.profiles (id, email, full_name, nip, role, is_active, school_name)
    VALUES (NEW.id, COALESCE(NEW.email, ''), meta_name, meta_nip, assigned_role, TRUE, meta_school)
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Cegah eskalasi hak akses mandiri pada tabel profiles
CREATE OR REPLACE FUNCTION public.prevent_self_privilege_escalation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    actor_role TEXT;
BEGIN
    -- Izinkan service_role / SQL Editor (auth.uid() IS NULL)
    IF auth.uid() IS NULL THEN
        RETURN NEW;
    END IF;

    actor_role := public.current_user_role();

    IF actor_role = 'super_admin' THEN
        RETURN NEW;
    ELSIF actor_role = 'admin' THEN
        -- Admin tidak boleh membuat/mengubah Super Admin
        IF OLD.role = 'super_admin' OR NEW.role = 'super_admin' THEN
            RAISE EXCEPTION 'Hanya Super Admin yang dapat mengelola akun Super Admin';
        END IF;
        RETURN NEW;
    ELSE
        -- Guru / Siswa tidak boleh mengubah role atau is_active
        IF NEW.role IS DISTINCT FROM OLD.role OR NEW.is_active IS DISTINCT FROM OLD.is_active THEN
            RAISE EXCEPTION 'Anda tidak memiliki izin untuk mengubah peran atau status aktif';
        END IF;
        RETURN NEW;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_prevent_escalation ON public.profiles;
CREATE TRIGGER trg_profiles_prevent_escalation
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.prevent_self_privilege_escalation();

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ============================================================================
-- 2. TABEL DATA INTI ADMINISTRASI PAI
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.school_data (
    id BIGINT PRIMARY KEY DEFAULT 1,
    school_name TEXT NOT NULL,
    npsn TEXT NOT NULL DEFAULT '',
    address TEXT NOT NULL DEFAULT '',
    headmaster_name TEXT NOT NULL DEFAULT '',
    academic_year TEXT NOT NULL DEFAULT '2026/2027',
    semester TEXT NOT NULL DEFAULT 'Ganjil',
    curriculum TEXT NOT NULL DEFAULT 'Kurikulum Merdeka',
    updated_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.teacher_profiles (
    id BIGINT PRIMARY KEY DEFAULT 1,
    user_id UUID REFERENCES auth.users(id),
    full_name TEXT NOT NULL,
    nip TEXT NOT NULL DEFAULT '',
    nuptk TEXT NOT NULL DEFAULT '',
    cert_no TEXT NOT NULL DEFAULT '',
    rank TEXT NOT NULL DEFAULT '',
    education TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    phone TEXT NOT NULL DEFAULT '',
    employment_status TEXT NOT NULL DEFAULT 'PPPK',
    subject_taught TEXT NOT NULL DEFAULT 'Pendidikan Agama Islam dan Budi Pekerti',
    work_unit TEXT NOT NULL DEFAULT 'SD Negeri Kiarapandak 01',
    additional_duties TEXT NOT NULL DEFAULT '',
    birth_place_date TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.class_rooms (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    level TEXT NOT NULL,
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.students (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    class_id BIGINT NOT NULL DEFAULT 1,
    class_sync_key TEXT NOT NULL DEFAULT '',
    nis TEXT NOT NULL DEFAULT '',
    name TEXT NOT NULL,
    gender TEXT NOT NULL DEFAULT 'L' CHECK (gender IN ('L', 'P')),
    parent_name TEXT NOT NULL DEFAULT '',
    parent_phone TEXT NOT NULL DEFAULT '',
    nisn TEXT NOT NULL DEFAULT '',
    birth_place TEXT NOT NULL DEFAULT '',
    birth_date TEXT NOT NULL DEFAULT '',
    address TEXT NOT NULL DEFAULT '',
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.merdeka_documents (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    subject TEXT NOT NULL DEFAULT 'Pendidikan Agama Islam dan Budi Pekerti',
    class_level TEXT NOT NULL DEFAULT 'Semua',
    content_text TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    file_uri TEXT,
    file_name TEXT,
    file_size TEXT,
    file_mime_type TEXT,
    storage_path TEXT,
    semester TEXT NOT NULL DEFAULT 'Ganjil',
    academic_year TEXT NOT NULL DEFAULT '2026/2027',
    author_teacher TEXT NOT NULL DEFAULT '',
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.teaching_journals (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    date TEXT NOT NULL,
    class_id BIGINT NOT NULL DEFAULT 1,
    class_name TEXT NOT NULL,
    topic TEXT NOT NULL,
    activity TEXT NOT NULL,
    teacher_present BOOLEAN NOT NULL DEFAULT TRUE,
    student_presence_json TEXT NOT NULL DEFAULT '',
    location_gps TEXT,
    photo_path TEXT,
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.assessment_records (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    student_id BIGINT NOT NULL DEFAULT 0,
    student_name TEXT NOT NULL,
    class_id BIGINT NOT NULL DEFAULT 1,
    type TEXT NOT NULL,
    topic TEXT NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    description TEXT NOT NULL DEFAULT '',
    date BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.questions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    text TEXT NOT NULL,
    option_a TEXT NOT NULL DEFAULT '',
    option_b TEXT NOT NULL DEFAULT '',
    option_c TEXT NOT NULL DEFAULT '',
    option_d TEXT NOT NULL DEFAULT '',
    answer_key TEXT NOT NULL DEFAULT 'A',
    difficulty TEXT NOT NULL DEFAULT 'Sedang',
    type TEXT NOT NULL DEFAULT 'Pilihan Ganda',
    explanation TEXT NOT NULL DEFAULT '',
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.religious_activities (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    date TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL,
    participant_count INTEGER NOT NULL DEFAULT 0 CHECK (participant_count >= 0),
    photo_path TEXT,
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.extracurriculars (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    mentor_name TEXT NOT NULL,
    schedule TEXT NOT NULL,
    participants_count INTEGER NOT NULL DEFAULT 0 CHECK (participants_count >= 0),
    description TEXT NOT NULL DEFAULT '',
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.attendance_records (
    id TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    class_id BIGINT NOT NULL,
    class_name TEXT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name TEXT NOT NULL,
    gender TEXT NOT NULL DEFAULT 'L',
    status TEXT NOT NULL DEFAULT 'HADIR',
    notes TEXT NOT NULL DEFAULT '',
    subject TEXT NOT NULL DEFAULT 'PAI & Budi Pekerti',
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.hafalan_records (
    id TEXT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    student_name TEXT NOT NULL,
    class_id BIGINT NOT NULL,
    class_name TEXT NOT NULL,
    surah_number INTEGER NOT NULL,
    surah_name TEXT NOT NULL,
    date TEXT NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    status TEXT NOT NULL DEFAULT 'MUMTAZ',
    kelancaran TEXT NOT NULL DEFAULT 'Sangat Lancar',
    tajwid TEXT NOT NULL DEFAULT 'Sangat Baik',
    makhraj TEXT NOT NULL DEFAULT 'Fasih',
    tester_teacher TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    created_by UUID REFERENCES auth.users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.sync_messages (
    id TEXT PRIMARY KEY,
    sender_id UUID REFERENCES auth.users(id),
    sender_nip TEXT NOT NULL DEFAULT '',
    sender_name TEXT NOT NULL,
    receiver_nip TEXT NOT NULL DEFAULT 'ALL',
    message_text TEXT NOT NULL,
    file_uri TEXT,
    file_type TEXT NOT NULL DEFAULT 'TEXT',
    file_name TEXT,
    file_size TEXT,
    location_latitude DOUBLE PRECISION,
    location_longitude DOUBLE PRECISION,
    duration_seconds INTEGER,
    timestamp BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'SENT',
    connection_method TEXT NOT NULL DEFAULT 'INTERNET_CLOUD',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.digital_archives (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Umum',
    file_name TEXT NOT NULL,
    file_size TEXT NOT NULL DEFAULT '',
    mime_type TEXT NOT NULL DEFAULT 'application/octet-stream',
    storage_path TEXT NOT NULL,
    public_url TEXT NOT NULL DEFAULT '',
    uploaded_by UUID REFERENCES auth.users(id),
    uploaded_by_name TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.activity_logs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    sync_key TEXT UNIQUE NOT NULL,
    user_id UUID REFERENCES auth.users(id),
    user_email TEXT NOT NULL DEFAULT '',
    user_name TEXT NOT NULL DEFAULT '',
    user_type TEXT NOT NULL,
    text TEXT NOT NULL,
    device_info TEXT NOT NULL DEFAULT 'Android',
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indeks performa query
CREATE INDEX IF NOT EXISTS idx_students_class_id ON public.students(class_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date_class ON public.attendance_records(date, class_id);
CREATE INDEX IF NOT EXISTS idx_hafalan_student ON public.hafalan_records(student_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_ts ON public.activity_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sync_messages_ts ON public.sync_messages(timestamp ASC);
CREATE INDEX IF NOT EXISTS idx_digital_archives_ts ON public.digital_archives(created_at DESC);

-- Trigger updated_at pada tabel domain
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY[
        'school_data', 'teacher_profiles', 'class_rooms', 'students',
        'merdeka_documents', 'teaching_journals', 'assessment_records',
        'questions', 'religious_activities', 'extracurriculars',
        'attendance_records', 'hafalan_records', 'sync_messages', 'digital_archives'
    ]
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON public.%I;', tbl, tbl);
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();', tbl, tbl);
    END LOOP;
END $$;

-- ============================================================================
-- 3. AKTIVASI ROW LEVEL SECURITY (RLS) PADA SELURUH TABEL
-- ============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teacher_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.class_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.merdeka_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teaching_journals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.assessment_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.religious_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.extracurriculars ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hafalan_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sync_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.digital_archives ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;

-- Policies: profiles
DROP POLICY IF EXISTS "profiles_select_policy" ON public.profiles;
CREATE POLICY "profiles_select_policy" ON public.profiles
    FOR SELECT TO authenticated
    USING (id = auth.uid() OR public.is_staff());

DROP POLICY IF EXISTS "profiles_update_policy" ON public.profiles;
CREATE POLICY "profiles_update_policy" ON public.profiles
    FOR UPDATE TO authenticated
    USING (id = auth.uid() OR public.is_admin())
    WITH CHECK (id = auth.uid() OR public.is_admin());

DROP POLICY IF EXISTS "profiles_delete_policy" ON public.profiles;
CREATE POLICY "profiles_delete_policy" ON public.profiles
    FOR DELETE TO authenticated
    USING (public.is_super_admin() AND id <> auth.uid());

-- Policies: school_data & teacher_profiles
DROP POLICY IF EXISTS "school_data_select" ON public.school_data;
CREATE POLICY "school_data_select" ON public.school_data FOR SELECT TO authenticated USING (public.is_active_user());
DROP POLICY IF EXISTS "school_data_write" ON public.school_data;
CREATE POLICY "school_data_write" ON public.school_data FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "teacher_profiles_select" ON public.teacher_profiles;
CREATE POLICY "teacher_profiles_select" ON public.teacher_profiles FOR SELECT TO authenticated USING (public.is_active_user());
DROP POLICY IF EXISTS "teacher_profiles_write" ON public.teacher_profiles;
CREATE POLICY "teacher_profiles_write" ON public.teacher_profiles FOR ALL TO authenticated USING (public.is_staff()) WITH CHECK (public.is_staff());

-- Macro-like policies untuk tabel akademik & administrasi (Siswa: Read-Only; Guru/Admin/Super Admin: Read/Write)
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY[
        'class_rooms', 'students', 'merdeka_documents', 'teaching_journals',
        'assessment_records', 'questions', 'religious_activities',
        'extracurriculars', 'attendance_records', 'hafalan_records', 'digital_archives'
    ]
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS "%I_select_active" ON public.%I;', tbl, tbl);
        EXECUTE format('CREATE POLICY "%I_select_active" ON public.%I FOR SELECT TO authenticated USING (public.is_active_user());', tbl, tbl);

        EXECUTE format('DROP POLICY IF EXISTS "%I_insert_staff" ON public.%I;', tbl, tbl);
        EXECUTE format('CREATE POLICY "%I_insert_staff" ON public.%I FOR INSERT TO authenticated WITH CHECK (public.is_staff());', tbl, tbl);

        EXECUTE format('DROP POLICY IF EXISTS "%I_update_staff" ON public.%I;', tbl, tbl);
        EXECUTE format('CREATE POLICY "%I_update_staff" ON public.%I FOR UPDATE TO authenticated USING (public.is_staff()) WITH CHECK (public.is_staff());', tbl, tbl);

        EXECUTE format('DROP POLICY IF EXISTS "%I_delete_staff" ON public.%I;', tbl, tbl);
        EXECUTE format('CREATE POLICY "%I_delete_staff" ON public.%I FOR DELETE TO authenticated USING (public.is_staff());', tbl, tbl);
    END LOOP;
END $$;

-- Policies: sync_messages (semua pengguna aktif dapat membaca dan mengirim pesan)
DROP POLICY IF EXISTS "sync_messages_select" ON public.sync_messages;
CREATE POLICY "sync_messages_select" ON public.sync_messages FOR SELECT TO authenticated USING (public.is_active_user());
DROP POLICY IF EXISTS "sync_messages_insert" ON public.sync_messages;
CREATE POLICY "sync_messages_insert" ON public.sync_messages FOR INSERT TO authenticated WITH CHECK (public.is_active_user() AND (sender_id IS NULL OR sender_id = auth.uid()));
DROP POLICY IF EXISTS "sync_messages_update" ON public.sync_messages;
CREATE POLICY "sync_messages_update" ON public.sync_messages FOR UPDATE TO authenticated USING (public.is_active_user()) WITH CHECK (public.is_active_user());
DROP POLICY IF EXISTS "sync_messages_delete" ON public.sync_messages;
CREATE POLICY "sync_messages_delete" ON public.sync_messages FOR DELETE TO authenticated USING (sender_id = auth.uid() OR public.is_admin());

-- Policies: activity_logs (semua user aktif bisa mencatat aktivitasnya; Admin/Super Admin melihat semua, non-admin melihat miliknya)
DROP POLICY IF EXISTS "activity_logs_select" ON public.activity_logs;
CREATE POLICY "activity_logs_select" ON public.activity_logs
    FOR SELECT TO authenticated
    USING (public.is_staff() OR user_id = auth.uid());

DROP POLICY IF EXISTS "activity_logs_insert" ON public.activity_logs;
CREATE POLICY "activity_logs_insert" ON public.activity_logs
    FOR INSERT TO authenticated
    WITH CHECK (public.is_active_user() AND (user_id IS NULL OR user_id = auth.uid()));

DROP POLICY IF EXISTS "activity_logs_delete" ON public.activity_logs;
CREATE POLICY "activity_logs_delete" ON public.activity_logs
    FOR DELETE TO authenticated
    USING (public.is_super_admin());

-- ============================================================================
-- 4. KONFIGURASI SUPABASE REALTIME UNTUK SINKRONISASI ANTARPERANGKAT
-- ============================================================================
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY[
        'profiles', 'school_data', 'teacher_profiles', 'class_rooms', 'students',
        'merdeka_documents', 'teaching_journals', 'assessment_records',
        'questions', 'religious_activities', 'extracurriculars',
        'attendance_records', 'hafalan_records', 'sync_messages',
        'digital_archives', 'activity_logs'
    ]
    LOOP
        BEGIN
            EXECUTE format('ALTER TABLE public.%I REPLICA IDENTITY FULL;', tbl);
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I;', tbl);
        EXCEPTION WHEN duplicate_object THEN
            NULL;
        END;
    END LOOP;
END $$;

-- ============================================================================
-- 5. SUPABASE STORAGE BUCKET & RLS POLICIES (pai-documents)
-- ============================================================================
INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES ('pai-documents', 'pai-documents', FALSE, 26214400) -- 25 MB limit
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "pai_docs_storage_select" ON storage.objects;
CREATE POLICY "pai_docs_storage_select" ON storage.objects
    FOR SELECT TO authenticated
    USING (bucket_id = 'pai-documents' AND public.is_active_user());

DROP POLICY IF EXISTS "pai_docs_storage_insert" ON storage.objects;
CREATE POLICY "pai_docs_storage_insert" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'pai-documents' AND public.is_staff());

DROP POLICY IF EXISTS "pai_docs_storage_update" ON storage.objects;
CREATE POLICY "pai_docs_storage_update" ON storage.objects
    FOR UPDATE TO authenticated
    USING (bucket_id = 'pai-documents' AND public.is_staff())
    WITH CHECK (bucket_id = 'pai-documents' AND public.is_staff());

DROP POLICY IF EXISTS "pai_docs_storage_delete" ON storage.objects;
CREATE POLICY "pai_docs_storage_delete" ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id = 'pai-documents' AND public.is_staff());
