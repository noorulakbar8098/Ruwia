-- =============================================================
-- Ruwia / Neer Thuli — PRODUCTION MULTI-TENANT SCHEMA (FIXED ORDER)
-- =============================================================

-- ─── 1. Setup Extensions ─────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── 2. Profiles Table (MUST BE FIRST) ───────────────────────
-- We establish the table structure before defining functions that reference it.
CREATE TABLE IF NOT EXISTS profiles (
    id         UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    role       TEXT NOT NULL DEFAULT 'admin',
    full_name  TEXT,
    phone      TEXT,
    admin_id   UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Ensure admin_id column exists if table was created previously
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='admin_id') THEN
        ALTER TABLE profiles ADD COLUMN admin_id UUID REFERENCES auth.users(id);
    END IF;
END $$;

-- ─── 3. Multi-Tenant Helper Functions ────────────────────────

-- Helper to get the admin_id for the current authenticated user.
-- Defined AFTER profiles table exists.
CREATE OR REPLACE FUNCTION public.get_admin_id()
RETURNS UUID AS $$
DECLARE
    res UUID;
BEGIN
    SELECT admin_id INTO res FROM public.profiles WHERE id = auth.uid();
    IF res IS NULL OR res = auth.uid() THEN
        SELECT admin_id INTO res FROM public.employees WHERE id = auth.uid();
    END IF;
    RETURN COALESCE(res, auth.uid());
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- Trigger to handle new user signup.
-- SECURITY: never trust role/admin_id from client-supplied metadata. Doing so
-- let anyone join an arbitrary tenant by passing its admin_id during signup.
-- Every self-signup becomes the admin of its own tenant; employee accounts are
-- created by the admin-users Edge Function, which moves them into the caller's
-- tenant after verifying the caller is an admin.
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

-- Trigger to automatically tag every new record with the correct admin_id
CREATE OR REPLACE FUNCTION public.inject_admin_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.admin_id IS NULL THEN
        NEW.admin_id := public.get_admin_id();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Migration helper: Add admin_id column and trigger to existing tables
CREATE OR REPLACE FUNCTION migrate_to_tenant_table(t_name TEXT)
RETURNS VOID AS $$
DECLARE
    tg_name TEXT;
BEGIN
    -- 1. Drop all other triggers on the table to clean up legacy multipliers
    FOR tg_name IN (
        SELECT tgname
        FROM pg_trigger
        JOIN pg_class ON pg_class.oid = tgrelid
        JOIN pg_namespace ON pg_namespace.oid = relnamespace
        WHERE relname = t_name
          AND nspname = 'public'
          AND tgname != 'trigger_inject_admin_id'
          AND tgisinternal = false
    ) LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS %I ON %I CASCADE', tg_name, t_name);
    END LOOP;

    -- 2. Add admin_id column if missing
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id)', t_name);

    -- 3. Add the injection trigger
    EXECUTE format('DROP TRIGGER IF EXISTS trigger_inject_admin_id ON %I', t_name);
    EXECUTE format('CREATE TRIGGER trigger_inject_admin_id BEFORE INSERT ON %I FOR EACH ROW EXECUTE FUNCTION public.inject_admin_id()', t_name);

    -- 4. Enable RLS
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t_name);

    -- 5. Clean up ALL old policies
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_rw', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_multi_tenant', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_read', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_admin', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_isolation', t_name);

    -- 6. Create strict isolation policy
    EXECUTE format(
        'CREATE POLICY %I ON %I FOR ALL TO authenticated
         USING (admin_id = public.get_admin_id())
         WITH CHECK (admin_id = public.get_admin_id())',
        t_name || '_isolation', t_name
    );
END;
$$ LANGUAGE plpgsql;

-- ─── 4. Operational Tables & Migration ───────────────────────

CREATE TABLE IF NOT EXISTS product_categories (
    id               UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name             TEXT NOT NULL,
    display_name     TEXT NOT NULL,
    brand_name       TEXT DEFAULT '',
    supplier_group   TEXT NOT NULL DEFAULT 'GC',
    purchase_price   DECIMAL(10,2) DEFAULT 0,
    purchase_price_gc  DECIMAL(10,2) DEFAULT 0,
    purchase_price_mb  DECIMAL(10,2) DEFAULT 0,
    default_sell_price DECIMAL(10,2) DEFAULT 0,
    stock_available  INT DEFAULT 0,
    low_stock_alert  INT DEFAULT 5,
    is_active        BOOLEAN DEFAULT true,
    is_deleted       BOOLEAN DEFAULT false,
    created_at       TIMESTAMPTZ DEFAULT now()
);
CREATE TABLE IF NOT EXISTS shop_stocks (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, name TEXT, location TEXT, total_cans INT, full_cans INT, empty_cans INT, cans_with_customers INT, is_live BOOLEAN, updated_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS suppliers (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, name TEXT, location TEXT, is_active BOOLEAN, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS customers (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, user_id UUID, name TEXT, phone TEXT, address TEXT, other_details TEXT, cans_held INT, balance DECIMAL, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS orders (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, customer_id UUID, qty INT, status TEXT, employee_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS employees (id UUID PRIMARY KEY, name TEXT, phone TEXT, status TEXT, role TEXT, shop_name TEXT, completed_deliveries INT, total_deliveries INT, monthly_salary DECIMAL, rating DECIMAL, today_stat INT, stat_label TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS stock_movements (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, source TEXT, qty INT, type TEXT, shop_name TEXT, employee_id UUID, product_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS sale_entries (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, date TEXT, customer_name TEXT, product_id TEXT, product_name TEXT, qty INT, purchase_price_per_unit DECIMAL, selling_price_per_unit DECIMAL, sales_margin_per_unit DECIMAL, total_selling DECIMAL, total_margin DECIMAL, shop_id TEXT, employee_id UUID, payment_received TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS monthly_expenses (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, month TEXT, shop_id TEXT, shop_rent DECIMAL, admin_salary DECIMAL, delivery_staff DECIMAL, miscellaneous DECIMAL, bike_expense DECIMAL, custom_expenses TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS outward (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, order_id UUID, customer_id UUID, qty_delivered INT, qty_empty_returned INT, rate DECIMAL, employee_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS payments (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, customer_id UUID, amount DECIMAL, mode TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS route_tasks (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, customer_name TEXT, phone TEXT, address TEXT, can_qty INT, eta_text TEXT, status TEXT, employee_id UUID, order_id UUID, scheduled_date DATE, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS app_settings (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, settings_key TEXT NOT NULL, settings_value TEXT, updated_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS customer_product_prices (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    customer_id UUID NOT NULL,
    product_id UUID NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Migrate all tables
SELECT migrate_to_tenant_table('product_categories');
ALTER TABLE product_categories ADD COLUMN IF NOT EXISTS brand_name TEXT DEFAULT '';
ALTER TABLE product_categories ADD COLUMN IF NOT EXISTS purchase_price DECIMAL(10,2) DEFAULT 0;
ALTER TABLE product_categories ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false;
ALTER TABLE product_categories ADD COLUMN IF NOT EXISTS low_stock_alert INT DEFAULT 5;
SELECT migrate_to_tenant_table('shop_stocks');
SELECT migrate_to_tenant_table('suppliers');
SELECT migrate_to_tenant_table('customers');
SELECT migrate_to_tenant_table('orders');
SELECT migrate_to_tenant_table('employees');
SELECT migrate_to_tenant_table('stock_movements');
SELECT migrate_to_tenant_table('sale_entries');
SELECT migrate_to_tenant_table('monthly_expenses');
SELECT migrate_to_tenant_table('outward');
SELECT migrate_to_tenant_table('payments');
SELECT migrate_to_tenant_table('route_tasks');
SELECT migrate_to_tenant_table('app_settings');
SELECT migrate_to_tenant_table('customer_product_prices');

-- Write-time snapshots + sale idempotency (safe to re-run: IF NOT EXISTS).
-- product_name keeps history showing real names after soft-delete;
-- client_key lets retried sales skip already-written lines instead of
-- deducting stock twice.
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS product_name TEXT;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS client_key TEXT;
ALTER TABLE sale_entries ADD COLUMN IF NOT EXISTS client_key TEXT;
ALTER TABLE sale_entries ADD COLUMN IF NOT EXISTS payment_received TEXT;

-- Delivery tasks store the structured customer/product/schedule the admin
-- flow writes. Older installs only had the free-text route columns.
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS customer_address TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS employee_name TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS product_id UUID;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS product_name TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS delivery_date DATE;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS timeline JSONB DEFAULT '[]'::jsonb;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS dest_latitude DOUBLE PRECISION;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS dest_longitude DOUBLE PRECISION;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS dest_name TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS maps_link TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS current_latitude DOUBLE PRECISION;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS current_longitude DOUBLE PRECISION;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS current_location_name TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS journey_start_time TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS last_location_update_time TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS arrival_time TEXT;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS distance_remaining_meters DOUBLE PRECISION;
ALTER TABLE route_tasks ADD COLUMN IF NOT EXISTS live_eta TEXT;
ALTER TABLE route_tasks ALTER COLUMN status SET DEFAULT 'pending';

CREATE INDEX IF NOT EXISTS route_tasks_admin_schedule_idx
    ON route_tasks (admin_id, scheduled_date);
CREATE INDEX IF NOT EXISTS route_tasks_employee_schedule_idx
    ON route_tasks (admin_id, employee_id, scheduled_date);

-- Special case: Profiles RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "profiles_isolation" ON profiles;
DROP POLICY IF EXISTS "profiles_update_self" ON profiles;
DROP POLICY IF EXISTS "profiles_admin_update_tenant" ON profiles;
DROP POLICY IF EXISTS "profiles_read" ON profiles;
DROP POLICY IF EXISTS "profiles_update" ON profiles;

CREATE POLICY "profiles_isolation" ON profiles FOR SELECT TO authenticated
    USING (admin_id = public.get_admin_id() OR id = auth.uid());

CREATE POLICY "profiles_update_self" ON profiles FOR UPDATE TO authenticated
    USING (id = auth.uid()) WITH CHECK (id = auth.uid());

-- Only a tenant admin may edit another member's profile. SECURITY DEFINER
-- avoids recursive RLS evaluation on profiles.
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

CREATE POLICY "profiles_admin_update_tenant" ON profiles FOR UPDATE TO authenticated
    USING (public.is_tenant_admin() AND admin_id = public.get_admin_id())
    WITH CHECK (public.is_tenant_admin() AND admin_id = public.get_admin_id());

-- Sensitive profile columns are server-managed. Clients may edit display
-- fields, but can never change their role or tenant membership. The signup
-- flow's redundant "set admin" write is a no-op and still succeeds.
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

DROP TRIGGER IF EXISTS protect_profile_tenant_fields ON profiles;
CREATE TRIGGER protect_profile_tenant_fields
    BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION public.protect_profile_tenant_fields();

-- Cleanup unique constraint on monthly_expenses
ALTER TABLE monthly_expenses DROP CONSTRAINT IF EXISTS monthly_expenses_month_shop_id_key;
ALTER TABLE monthly_expenses DROP CONSTRAINT IF EXISTS monthly_expenses_month_shop_id_admin_id_key;
ALTER TABLE monthly_expenses ADD CONSTRAINT monthly_expenses_month_shop_id_admin_id_key UNIQUE (month, shop_id, admin_id);

-- ─── 5. Auxiliary Safe RPCs ──────────────────────────────────
-- Safe stock adjustment function that works with RLS and resolves JSON parsing EOF errors
CREATE OR REPLACE FUNCTION public.update_product_stock(
    p_id UUID,
    p_new_stock INT,
    p_expected_current INT
) RETURNS BOOLEAN AS $$
DECLARE
    v_updated_rows INT;
BEGIN
    UPDATE public.product_categories
    SET stock_available = p_new_stock
    WHERE id = p_id 
      AND stock_available = p_expected_current 
      AND admin_id = public.get_admin_id();
      
    GET DIAGNOSTICS v_updated_rows = ROW_COUNT;
    RETURN v_updated_rows > 0;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

