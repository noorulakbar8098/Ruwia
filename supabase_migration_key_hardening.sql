-- =============================================================
-- Ruwia — security hardening (run once in Supabase SQL Editor)
-- =============================================================
-- 1. Stops a signup from choosing its own role / tenant via user metadata.
-- 2. Stops authenticated users from changing profiles.role or profiles.admin_id.
-- 3. Lets a tenant admin update employee profile display fields.
--
-- The `admin-users` Edge Function creates employee accounts with the service
-- role and then moves them into the caller's tenant. Clients never hold that
-- key.

-- ─── 1. Signup trigger: ignore client-supplied role/admin_id ──
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, role, full_name, phone, admin_id)
    VALUES (
        NEW.id,
        'admin',
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'phone',
        NEW.id
    )
    ON CONFLICT (id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        phone = EXCLUDED.phone;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ─── 2. Tenant-admin helper ───────────────────────────────────
CREATE OR REPLACE FUNCTION public.is_tenant_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'admin'
    );
$$;

-- ─── 3. Profiles policies + column privileges ─────────────────
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "profiles_isolation" ON public.profiles;
DROP POLICY IF EXISTS "profiles_update_self" ON public.profiles;
DROP POLICY IF EXISTS "profiles_admin_update_tenant" ON public.profiles;
DROP POLICY IF EXISTS "profiles_read" ON public.profiles;
DROP POLICY IF EXISTS "profiles_update" ON public.profiles;

CREATE POLICY "profiles_isolation" ON public.profiles FOR SELECT TO authenticated
    USING (admin_id = public.get_admin_id() OR id = auth.uid());

CREATE POLICY "profiles_update_self" ON public.profiles FOR UPDATE TO authenticated
    USING (id = auth.uid()) WITH CHECK (id = auth.uid());

CREATE POLICY "profiles_admin_update_tenant" ON public.profiles FOR UPDATE TO authenticated
    USING (public.is_tenant_admin() AND admin_id = public.get_admin_id())
    WITH CHECK (public.is_tenant_admin() AND admin_id = public.get_admin_id());

-- ─── 4. Profile field protection ──────────────────────────────
-- Role and tenant membership are server-managed. Clients may still update
-- full_name/phone, and the old signup flow's no-op "set admin" write is
-- allowed because the value does not change.
CREATE OR REPLACE FUNCTION public.protect_profile_tenant_fields()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    IF current_user IN ('postgres', 'supabase_admin', 'service_role')
       OR auth.role() = 'service_role' THEN
        RETURN NEW;
    END IF;
    IF NEW.admin_id IS DISTINCT FROM OLD.admin_id THEN
        RAISE EXCEPTION 'admin_id is server-managed';
    END IF;
    IF NEW.role IS DISTINCT FROM OLD.role THEN
        RAISE EXCEPTION 'role is server-managed';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS protect_profile_tenant_fields ON public.profiles;
CREATE TRIGGER protect_profile_tenant_fields
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.protect_profile_tenant_fields();

-- ─── 5. Verification (read-only) ──────────────────────────────
-- Expect: one row for protect_profile_tenant_fields.
-- SELECT tgname FROM pg_trigger
-- WHERE tgrelid = 'public.profiles'::regclass AND NOT tgisinternal;
