-- ============================================================================
-- Migration 0001: Users Table, RBAC Roles & Row Level Security (RLS)
-- Roles: super_admin, admin, teacher, student
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Tabel public.users dengan kolom role tervalidasi
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL DEFAULT 'Pengguna',
    nip TEXT NOT NULL DEFAULT '',
    role TEXT NOT NULL DEFAULT 'student'
        CHECK (role IN ('super_admin', 'admin', 'teacher', 'student')),
    school_name TEXT NOT NULL DEFAULT 'SD Negeri Kiarapandak 01',
    class_name TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_role ON public.users(role);
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_is_active ON public.users(is_active);

-- ----------------------------------------------------------------------------
-- 2. Fungsi Helper Keamanan (SECURITY DEFINER untuk mencegah rekursi RLS)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.set_row_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.current_app_role()
RETURNS TEXT
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT role
    FROM public.users
    WHERE id = auth.uid()
      AND is_active = TRUE
    LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.is_active_app_user()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.users
        WHERE id = auth.uid()
          AND is_active = TRUE
    );
$$;

CREATE OR REPLACE FUNCTION public.is_super_admin_role()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_app_role() = 'super_admin', FALSE);
$$;

CREATE OR REPLACE FUNCTION public.is_admin_role()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_app_role() IN ('super_admin', 'admin'), FALSE);
$$;

CREATE OR REPLACE FUNCTION public.is_teacher_or_above()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_app_role() IN ('super_admin', 'admin', 'teacher'), FALSE);
$$;

CREATE OR REPLACE FUNCTION public.is_student_role()
RETURNS BOOLEAN
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(public.current_app_role() = 'student', FALSE);
$$;

-- ----------------------------------------------------------------------------
-- 3. Trigger Otomatis Pembuatan User & Pencegahan Eskalasi Hak Akses
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_auth_user_to_public_users()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    assigned_role TEXT := 'student';
    meta_name TEXT;
    meta_nip TEXT;
    meta_school TEXT;
BEGIN
    -- Akun bootstrap pemilik ditetapkan sebagai super_admin; pendaftar mandiri default 'student'
    IF LOWER(COALESCE(NEW.email, '')) = 'kkgpaikecamatansukajaya@gmail.com' THEN
        assigned_role := 'super_admin';
    ELSE
        assigned_role := 'student';
    END IF;

    meta_name := COALESCE(
        NULLIF(TRIM(NEW.raw_user_meta_data->>'full_name'), ''),
        SPLIT_PART(COALESCE(NEW.email, 'Pengguna'), '@', 1)
    );
    meta_nip := COALESCE(TRIM(NEW.raw_user_meta_data->>'nip'), '');
    meta_school := COALESCE(
        NULLIF(TRIM(NEW.raw_user_meta_data->>'school_name'), ''),
        'SD Negeri Kiarapandak 01'
    );

    INSERT INTO public.users (id, email, full_name, nip, role, school_name, is_active)
    VALUES (NEW.id, LOWER(COALESCE(NEW.email, '')), meta_name, meta_nip, assigned_role, meta_school, TRUE)
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_public_users ON auth.users;
CREATE TRIGGER on_auth_user_created_public_users
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_auth_user_to_public_users();

CREATE OR REPLACE FUNCTION public.prevent_users_privilege_escalation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    actor_role TEXT;
BEGIN
    -- Izinkan SQL Editor / service_role backend
    IF auth.uid() IS NULL THEN
        RETURN NEW;
    END IF;

    actor_role := public.current_app_role();

    IF actor_role = 'super_admin' THEN
        RETURN NEW;
    ELSIF actor_role = 'admin' THEN
        IF OLD.role = 'super_admin' OR NEW.role = 'super_admin' THEN
            RAISE EXCEPTION 'Admin tidak diizinkan mengubah atau menetapkan peran Super Admin';
        END IF;
        RETURN NEW;
    ELSE
        -- Teacher dan Student tidak boleh mengubah role, email, maupun is_active miliknya
        IF NEW.role IS DISTINCT FROM OLD.role
           OR NEW.is_active IS DISTINCT FROM OLD.is_active
           OR NEW.email IS DISTINCT FROM OLD.email THEN
            RAISE EXCEPTION 'Anda tidak memiliki izin untuk mengubah peran, status aktif, atau email akun';
        END IF;
        RETURN NEW;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_users_prevent_escalation ON public.users;
CREATE TRIGGER trg_users_prevent_escalation
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.prevent_users_privilege_escalation();

DROP TRIGGER IF EXISTS trg_users_updated_at ON public.users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.set_row_updated_at();

-- ----------------------------------------------------------------------------
-- 4. Row Level Security (RLS) pada tabel public.users
-- ----------------------------------------------------------------------------
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- SELECT:
-- - Super Admin & Admin: melihat semua pengguna
-- - Teacher: melihat seluruh direktori pengguna aktif (guru, admin, siswa) untuk pembelajaran
-- - Student: hanya melihat profil dirinya sendiri
DROP POLICY IF EXISTS "users_select_super_admin" ON public.users;
CREATE POLICY "users_select_super_admin" ON public.users
    FOR SELECT TO authenticated
    USING (public.is_super_admin_role());

DROP POLICY IF EXISTS "users_select_admin" ON public.users;
CREATE POLICY "users_select_admin" ON public.users
    FOR SELECT TO authenticated
    USING (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "users_select_teacher" ON public.users;
CREATE POLICY "users_select_teacher" ON public.users
    FOR SELECT TO authenticated
    USING (public.current_app_role() = 'teacher' AND is_active = TRUE);

DROP POLICY IF EXISTS "users_select_student_own" ON public.users;
CREATE POLICY "users_select_student_own" ON public.users
    FOR SELECT TO authenticated
    USING (public.current_app_role() = 'student' AND id = auth.uid());

-- INSERT:
-- - Super Admin: dapat membuat entri user dengan peran apa pun
-- - Admin: dapat membuat entri user selain super_admin
-- - Teacher & Student: ditolak (registrasi dilakukan via Supabase Auth trigger)
DROP POLICY IF EXISTS "users_insert_super_admin" ON public.users;
CREATE POLICY "users_insert_super_admin" ON public.users
    FOR INSERT TO authenticated
    WITH CHECK (public.is_super_admin_role());

DROP POLICY IF EXISTS "users_insert_admin" ON public.users;
CREATE POLICY "users_insert_admin" ON public.users
    FOR INSERT TO authenticated
    WITH CHECK (
        public.current_app_role() = 'admin'
        AND role IN ('admin', 'teacher', 'student')
    );

-- UPDATE:
-- - Super Admin: dapat mengubah semua user
-- - Admin: dapat mengubah user non-super_admin (dan tidak boleh menaikkan ke super_admin)
-- - Teacher & Student: hanya dapat memperbarui profil dirinya sendiri (trigger mencegah ubah role/is_active)
DROP POLICY IF EXISTS "users_update_super_admin" ON public.users;
CREATE POLICY "users_update_super_admin" ON public.users
    FOR UPDATE TO authenticated
    USING (public.is_super_admin_role())
    WITH CHECK (public.is_super_admin_role());

DROP POLICY IF EXISTS "users_update_admin" ON public.users;
CREATE POLICY "users_update_admin" ON public.users
    FOR UPDATE TO authenticated
    USING (public.current_app_role() = 'admin' AND role <> 'super_admin')
    WITH CHECK (public.current_app_role() = 'admin' AND role IN ('admin', 'teacher', 'student'));

DROP POLICY IF EXISTS "users_update_self_teacher_student" ON public.users;
CREATE POLICY "users_update_self_teacher_student" ON public.users
    FOR UPDATE TO authenticated
    USING (
        public.current_app_role() IN ('teacher', 'student')
        AND id = auth.uid()
    )
    WITH CHECK (
        public.current_app_role() IN ('teacher', 'student')
        AND id = auth.uid()
    );

-- DELETE:
-- - Super Admin: dapat menghapus akun selain dirinya sendiri
-- - Admin: dapat menghapus akun teacher/student saja
-- - Teacher & Student: ditolak
DROP POLICY IF EXISTS "users_delete_super_admin" ON public.users;
CREATE POLICY "users_delete_super_admin" ON public.users
    FOR DELETE TO authenticated
    USING (public.is_super_admin_role() AND id <> auth.uid());

DROP POLICY IF EXISTS "users_delete_admin" ON public.users;
CREATE POLICY "users_delete_admin" ON public.users
    FOR DELETE TO authenticated
    USING (
        public.current_app_role() = 'admin'
        AND role IN ('teacher', 'student')
        AND id <> auth.uid()
    );

-- Aktifkan Realtime pada tabel users
DO $$
BEGIN
    ALTER TABLE public.users REPLICA IDENTITY FULL;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.users;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;
