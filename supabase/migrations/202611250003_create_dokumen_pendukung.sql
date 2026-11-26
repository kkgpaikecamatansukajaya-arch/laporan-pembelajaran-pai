-- ============================================================================
-- Migration 0003: Dokumen Pendukung Table, Storage Bucket & Role-Based RLS
-- Bergantung pada: 202611250001_create_users_and_roles.sql &
--                  202611250002_create_administrasi_guru.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.dokumen_pendukung (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    administrasi_id UUID REFERENCES public.administrasi_guru(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL DEFAULT auth.uid() REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Umum',
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL UNIQUE,
    storage_bucket TEXT NOT NULL DEFAULT 'dokumen-pendukung',
    mime_type TEXT NOT NULL DEFAULT 'application/octet-stream',
    file_size_bytes BIGINT NOT NULL DEFAULT 0 CHECK (file_size_bytes >= 0),
    visibility TEXT NOT NULL DEFAULT 'staff'
        CHECK (visibility IN ('private', 'staff', 'students', 'public_school')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dok_pendukung_admin_id ON public.dokumen_pendukung(administrasi_id);
CREATE INDEX IF NOT EXISTS idx_dok_pendukung_uploader ON public.dokumen_pendukung(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_dok_pendukung_visibility ON public.dokumen_pendukung(visibility);
CREATE INDEX IF NOT EXISTS idx_dok_pendukung_category ON public.dokumen_pendukung(category);

DROP TRIGGER IF EXISTS trg_dokumen_pendukung_updated_at ON public.dokumen_pendukung;
CREATE TRIGGER trg_dokumen_pendukung_updated_at
    BEFORE UPDATE ON public.dokumen_pendukung
    FOR EACH ROW
    EXECUTE FUNCTION public.set_row_updated_at();

-- ----------------------------------------------------------------------------
-- Row Level Security (RLS) pada public.dokumen_pendukung
-- ----------------------------------------------------------------------------
ALTER TABLE public.dokumen_pendukung ENABLE ROW LEVEL SECURITY;

-- SELECT:
-- - Super Admin & Admin: melihat seluruh dokumen pendukung
-- - Teacher: melihat dokumen miliknya (termasuk private) serta dokumen dengan visibilitas staff/students/public_school
-- - Student: hanya melihat dokumen bervisibilitas students/public_school (dan bila terkait administrasi_guru, induknya harus published & shared)
DROP POLICY IF EXISTS "dok_pendukung_select_super_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_select_super_admin" ON public.dokumen_pendukung
    FOR SELECT TO authenticated
    USING (public.is_super_admin_role());

DROP POLICY IF EXISTS "dok_pendukung_select_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_select_admin" ON public.dokumen_pendukung
    FOR SELECT TO authenticated
    USING (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "dok_pendukung_select_teacher" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_select_teacher" ON public.dokumen_pendukung
    FOR SELECT TO authenticated
    USING (
        public.current_app_role() = 'teacher'
        AND (
            uploaded_by = auth.uid()
            OR visibility IN ('staff', 'students', 'public_school')
        )
    );

DROP POLICY IF EXISTS "dok_pendukung_select_student" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_select_student" ON public.dokumen_pendukung
    FOR SELECT TO authenticated
    USING (
        public.current_app_role() = 'student'
        AND visibility IN ('students', 'public_school')
        AND (
            administrasi_id IS NULL
            OR EXISTS (
                SELECT 1
                FROM public.administrasi_guru ag
                WHERE ag.id = dokumen_pendukung.administrasi_id
                  AND ag.status = 'published'
                  AND ag.is_shared_with_students = TRUE
            )
        )
    );

-- INSERT:
-- - Super Admin & Admin: dapat mengunggah/mencatat dokumen pendukung apa pun
-- - Teacher: dapat mengunggah dokumen miliknya sendiri; jika menautkan ke administrasi_guru, harus pemiliknya
-- - Student: ditolak
DROP POLICY IF EXISTS "dok_pendukung_insert_super_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_insert_super_admin" ON public.dokumen_pendukung
    FOR INSERT TO authenticated
    WITH CHECK (public.is_super_admin_role());

DROP POLICY IF EXISTS "dok_pendukung_insert_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_insert_admin" ON public.dokumen_pendukung
    FOR INSERT TO authenticated
    WITH CHECK (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "dok_pendukung_insert_teacher" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_insert_teacher" ON public.dokumen_pendukung
    FOR INSERT TO authenticated
    WITH CHECK (
        public.current_app_role() = 'teacher'
        AND uploaded_by = auth.uid()
        AND (
            administrasi_id IS NULL
            OR EXISTS (
                SELECT 1
                FROM public.administrasi_guru ag
                WHERE ag.id = dokumen_pendukung.administrasi_id
                  AND ag.owner_id = auth.uid()
            )
        )
    );

-- UPDATE:
-- - Super Admin & Admin: dapat memperbarui dokumen pendukung apa pun
-- - Teacher: hanya dapat memperbarui dokumen yang diunggahnya sendiri
-- - Student: ditolak
DROP POLICY IF EXISTS "dok_pendukung_update_super_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_update_super_admin" ON public.dokumen_pendukung
    FOR UPDATE TO authenticated
    USING (public.is_super_admin_role())
    WITH CHECK (public.is_super_admin_role());

DROP POLICY IF EXISTS "dok_pendukung_update_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_update_admin" ON public.dokumen_pendukung
    FOR UPDATE TO authenticated
    USING (public.current_app_role() = 'admin')
    WITH CHECK (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "dok_pendukung_update_teacher_own" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_update_teacher_own" ON public.dokumen_pendukung
    FOR UPDATE TO authenticated
    USING (
        public.current_app_role() = 'teacher'
        AND uploaded_by = auth.uid()
    )
    WITH CHECK (
        public.current_app_role() = 'teacher'
        AND uploaded_by = auth.uid()
        AND (
            administrasi_id IS NULL
            OR EXISTS (
                SELECT 1
                FROM public.administrasi_guru ag
                WHERE ag.id = dokumen_pendukung.administrasi_id
                  AND ag.owner_id = auth.uid()
            )
        )
    );

-- DELETE:
-- - Super Admin & Admin: dapat menghapus dokumen pendukung apa pun
-- - Teacher: hanya dapat menghapus dokumen yang diunggahnya sendiri
-- - Student: ditolak
DROP POLICY IF EXISTS "dok_pendukung_delete_super_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_delete_super_admin" ON public.dokumen_pendukung
    FOR DELETE TO authenticated
    USING (public.is_super_admin_role());

DROP POLICY IF EXISTS "dok_pendukung_delete_admin" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_delete_admin" ON public.dokumen_pendukung
    FOR DELETE TO authenticated
    USING (public.current_app_role() = 'admin');

DROP POLICY IF EXISTS "dok_pendukung_delete_teacher_own" ON public.dokumen_pendukung;
CREATE POLICY "dok_pendukung_delete_teacher_own" ON public.dokumen_pendukung
    FOR DELETE TO authenticated
    USING (
        public.current_app_role() = 'teacher'
        AND uploaded_by = auth.uid()
    );

-- ----------------------------------------------------------------------------
-- Supabase Storage Bucket & RLS untuk 'dokumen-pendukung'
-- ----------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES ('dokumen-pendukung', 'dokumen-pendukung', FALSE, 26214400)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "storage_dok_pendukung_select_staff" ON storage.objects;
CREATE POLICY "storage_dok_pendukung_select_staff" ON storage.objects
    FOR SELECT TO authenticated
    USING (
        bucket_id = 'dokumen-pendukung'
        AND (
            public.is_admin_role()
            OR (
                public.current_app_role() = 'teacher'
                AND (
                    owner = auth.uid()
                    OR EXISTS (
                        SELECT 1 FROM public.dokumen_pendukung dp
                        WHERE dp.file_path = storage.objects.name
                          AND (dp.uploaded_by = auth.uid() OR dp.visibility IN ('staff', 'students', 'public_school'))
                    )
                )
            )
            OR (
                public.current_app_role() = 'student'
                AND EXISTS (
                    SELECT 1 FROM public.dokumen_pendukung dp
                    WHERE dp.file_path = storage.objects.name
                      AND dp.visibility IN ('students', 'public_school')
                      AND (
                          dp.administrasi_id IS NULL
                          OR EXISTS (
                              SELECT 1 FROM public.administrasi_guru ag
                              WHERE ag.id = dp.administrasi_id
                                AND ag.status = 'published'
                                AND ag.is_shared_with_students = TRUE
                          )
                      )
                )
            )
        )
    );

DROP POLICY IF EXISTS "storage_dok_pendukung_insert_staff" ON storage.objects;
CREATE POLICY "storage_dok_pendukung_insert_staff" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'dokumen-pendukung'
        AND (
            public.is_admin_role()
            OR (
                public.current_app_role() = 'teacher'
                AND (storage.foldername(name))[1] = auth.uid()::TEXT
            )
        )
    );

DROP POLICY IF EXISTS "storage_dok_pendukung_update_staff" ON storage.objects;
CREATE POLICY "storage_dok_pendukung_update_staff" ON storage.objects
    FOR UPDATE TO authenticated
    USING (
        bucket_id = 'dokumen-pendukung'
        AND (
            public.is_admin_role()
            OR (public.current_app_role() = 'teacher' AND owner = auth.uid())
        )
    )
    WITH CHECK (
        bucket_id = 'dokumen-pendukung'
        AND (
            public.is_admin_role()
            OR (public.current_app_role() = 'teacher' AND owner = auth.uid())
        )
    );

DROP POLICY IF EXISTS "storage_dok_pendukung_delete_staff" ON storage.objects;
CREATE POLICY "storage_dok_pendukung_delete_staff" ON storage.objects
    FOR DELETE TO authenticated
    USING (
        bucket_id = 'dokumen-pendukung'
        AND (
            public.is_admin_role()
            OR (public.current_app_role() = 'teacher' AND owner = auth.uid())
        )
    );

-- Aktifkan Realtime pada tabel dokumen_pendukung
DO $$
BEGIN
    ALTER TABLE public.dokumen_pendukung REPLICA IDENTITY FULL;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.dokumen_pendukung;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;
