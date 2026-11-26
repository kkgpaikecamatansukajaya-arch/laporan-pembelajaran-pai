-- ============================================================================
-- Migration 0002: Administrasi Guru Table & Role-Based RLS Policies
-- Bergantung pada: 202611250001_create_users_and_roles.sql (public.users)
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.administrasi_guru (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL DEFAULT auth.uid() REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    doc_type TEXT NOT NULL
        CHECK (doc_type IN (
            'Modul Ajar', 'ATP', 'CP', 'Prota', 'Promes',
            'KKTP', 'LKPD', 'Jurnal Mengajar', 'Asesmen',
            'Program Kerja', 'Lainnya'
        )),
    subject TEXT NOT NULL DEFAULT 'Pendidikan Agama Islam dan Budi Pekerti',
    class_level TEXT NOT NULL DEFAULT 'Semua',
    semester TEXT NOT NULL DEFAULT 'Ganjil',
    academic_year TEXT NOT NULL DEFAULT '2026/2027',
    school_name TEXT NOT NULL DEFAULT 'SD Negeri Kiarapandak 01',
    content TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'submitted', 'approved', 'published')),
    is_shared_with_students BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_guru_owner ON public.administrasi_guru(owner_id);
CREATE INDEX IF NOT EXISTS idx_admin_guru_doc_type ON public.administrasi_guru(doc_type);
CREATE INDEX IF NOT EXISTS idx_admin_guru_status ON public.administrasi_guru(status);
CREATE INDEX IF NOT EXISTS idx_admin_guru_student_share
    ON public.administrasi_guru(status, is_shared_with_students);

DROP TRIGGER IF EXISTS trg_administrasi_guru_updated_at ON public.administrasi_guru;
CREATE TRIGGER trg_administrasi_guru_updated_at
    BEFORE UPDATE ON public.administrasi_guru
    FOR EACH ROW
    EXECUTE FUNCTION public.set_row_updated_at();

-- Validasi agar Teacher tidak memalsukan status persetujuan Admin/Super Admin
CREATE OR REPLACE FUNCTION public.validate_administrasi_guru_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    actor_role TEXT;
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN NEW;
    END IF;

    actor_role := public.current_app_role();

    IF actor_role IN ('super_admin', 'admin') THEN
        IF NEW.status = 'approved' AND (TG_OP = 'INSERT' OR OLD.status IS DISTINCT FROM 'approved') THEN
            NEW.approved_by := COALESCE(NEW.approved_by, auth.uid());
            NEW.approved_at := COALESCE(NEW.approved_at, NOW());
        END IF;
        RETURN NEW;
    END IF;

    -- Teacher tidak boleh mengubah kepemilikan atau menyetujui dokumennya sendiri
    IF TG_OP = 'UPDATE' AND NEW.owner_id IS DISTINCT FROM OLD.owner_id THEN
        RAISE EXCEPTION 'Hanya Admin atau Super Admin yang dapat memindahkan kepemilikan dokumen';
    END IF;

    IF NEW.status = 'approved' AND (TG_OP = 'INSERT' OR OLD.status IS DISTINCT FROM 'approved') THEN
        RAISE EXCEPTION 'Status approved hanya dapat ditetapkan oleh Admin atau Super Admin';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_administrasi_guru_validate ON public.administrasi_guru;
CREATE TRIGGER trg_administrasi_guru_validate
    BEFORE INSERT OR UPDATE ON public.administrasi_guru
    FOR EACH ROW
    EXECUTE FUNCTION public.validate_administrasi_guru_mutation();

-- ----------------------------------------------------------------------------
-- Row Level Security (RLS) pada public.administrasi_guru
-- ----------------------------------------------------------------------------
ALTER TABLE public.administrasi_guru ENABLE ROW LEVEL SECURITY;

-- SELECT:
-- - Super Admin & Admin: melihat seluruh dokumen administrasi guru
-- - Teacher: melihat dokumen miliknya sendiri serta dokumen berstatus approved/published
-- - Student: hanya melihat materi yang berstatus published & dibagikan ke siswa
DROP POLICY IF EXISTS "admin_guru_select_super_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_select_super_admin" ON public.administrasi_guru
    FOR SELECT TO authenticated
    USING (public.is_super_admin_role());

DROP POLICY IF EXISTS "admin_guru_select_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_select_admin" ON public.administrasi_guru
    FOR SELECT TO authenticated
    USING (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "admin_guru_select_teacher" ON public.administrasi_guru;
CREATE POLICY "admin_guru_select_teacher" ON public.administrasi_guru
    FOR SELECT TO authenticated
    USING (
        public.current_app_role() = 'teacher'
        AND (owner_id = auth.uid() OR status IN ('approved', 'published'))
    );

DROP POLICY IF EXISTS "admin_guru_select_student" ON public.administrasi_guru;
CREATE POLICY "admin_guru_select_student" ON public.administrasi_guru
    FOR SELECT TO authenticated
    USING (
        public.current_app_role() = 'student'
        AND status = 'published'
        AND is_shared_with_students = TRUE
    );

-- INSERT:
-- - Super Admin & Admin: dapat membuat dokumen administrasi guru untuk siapa saja
-- - Teacher: dapat membuat dokumen miliknya sendiri (owner_id = auth.uid())
-- - Student: ditolak
DROP POLICY IF EXISTS "admin_guru_insert_super_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_insert_super_admin" ON public.administrasi_guru
    FOR INSERT TO authenticated
    WITH CHECK (public.is_super_admin_role());

DROP POLICY IF EXISTS "admin_guru_insert_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_insert_admin" ON public.administrasi_guru
    FOR INSERT TO authenticated
    WITH CHECK (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "admin_guru_insert_teacher" ON public.administrasi_guru;
CREATE POLICY "admin_guru_insert_teacher" ON public.administrasi_guru
    FOR INSERT TO authenticated
    WITH CHECK (
        public.current_app_role() = 'teacher'
        AND owner_id = auth.uid()
    );

-- UPDATE:
-- - Super Admin & Admin: dapat memperbarui dan menyetujui dokumen apa pun
-- - Teacher: hanya dapat memperbarui dokumen miliknya sendiri
-- - Student: ditolak
DROP POLICY IF EXISTS "admin_guru_update_super_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_update_super_admin" ON public.administrasi_guru
    FOR UPDATE TO authenticated
    USING (public.is_super_admin_role())
    WITH CHECK (public.is_super_admin_role());

DROP POLICY IF EXISTS "admin_guru_update_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_update_admin" ON public.administrasi_guru
    FOR UPDATE TO authenticated
    USING (public.current_app_role() = 'admin')
    WITH CHECK (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "admin_guru_update_teacher_own" ON public.administrasi_guru;
CREATE POLICY "admin_guru_update_teacher_own" ON public.administrasi_guru
    FOR UPDATE TO authenticated
    USING (
        public.current_app_role() = 'teacher'
        AND owner_id = auth.uid()
    )
    WITH CHECK (
        public.current_app_role() = 'teacher'
        AND owner_id = auth.uid()
    );

-- DELETE:
-- - Super Admin & Admin: dapat menghapus dokumen apa pun
-- - Teacher: hanya dapat menghapus dokumen miliknya sendiri (draft/submitted/published)
-- - Student: ditolak
DROP POLICY IF EXISTS "admin_guru_delete_super_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_delete_super_admin" ON public.administrasi_guru
    FOR DELETE TO authenticated
    USING (public.is_super_admin_role());

DROP POLICY IF EXISTS "admin_guru_delete_admin" ON public.administrasi_guru;
CREATE POLICY "admin_guru_delete_admin" ON public.administrasi_guru
    FOR DELETE TO authenticated
    USING (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "admin_guru_delete_teacher_own" ON public.administrasi_guru;
CREATE POLICY "admin_guru_delete_teacher_own" ON public.administrasi_guru
    FOR DELETE TO authenticated
    USING (
        public.current_app_role() = 'teacher'
        AND owner_id = auth.uid()
    );

-- Aktifkan Realtime pada tabel administrasi_guru
DO $$
BEGIN
    ALTER TABLE public.administrasi_guru REPLICA IDENTITY FULL;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.administrasi_guru;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;
