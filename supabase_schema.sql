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
    SELECT admin_id FROM public.profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Trigger to handle new user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    assigned_role TEXT;
    assigned_admin_id UUID;
BEGIN
    assigned_role := COALESCE(NEW.raw_user_meta_data->>'role', 'admin');
    assigned_admin_id := COALESCE((NEW.raw_user_meta_data->>'admin_id')::UUID, NEW.id);

    INSERT INTO public.profiles (id, role, full_name, phone, admin_id)
    VALUES (
        NEW.id,
        assigned_role,
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'phone',
        assigned_admin_id
    )
    ON CONFLICT (id) DO UPDATE SET
        role = EXCLUDED.role,
        full_name = EXCLUDED.full_name,
        phone = EXCLUDED.phone,
        admin_id = EXCLUDED.admin_id;
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
BEGIN
    -- 1. Add admin_id column if missing
    EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id)', t_name);

    -- 2. Add the injection trigger
    EXECUTE format('DROP TRIGGER IF EXISTS trigger_inject_admin_id ON %I', t_name);
    EXECUTE format('CREATE TRIGGER trigger_inject_admin_id BEFORE INSERT ON %I FOR EACH ROW EXECUTE FUNCTION public.inject_admin_id()', t_name);

    -- 3. Enable RLS
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t_name);

    -- 4. Clean up ALL old policies
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_rw', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_multi_tenant', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_read', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_admin', t_name);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', t_name || '_isolation', t_name);

    -- 5. Create strict isolation policy
    EXECUTE format(
        'CREATE POLICY %I ON %I FOR ALL TO authenticated
         USING (admin_id = public.get_admin_id())
         WITH CHECK (admin_id = public.get_admin_id())',
        t_name || '_isolation', t_name
    );
END;
$$ LANGUAGE plpgsql;

-- ─── 4. Operational Tables & Migration ───────────────────────

CREATE TABLE IF NOT EXISTS product_categories (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, name TEXT, display_name TEXT, supplier_group TEXT, purchase_price_gc DECIMAL, purchase_price_mb DECIMAL, default_sell_price DECIMAL, stock_available INT, is_active BOOLEAN, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS shop_stocks (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, name TEXT, location TEXT, total_cans INT, full_cans INT, empty_cans INT, cans_with_customers INT, is_live BOOLEAN, updated_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS suppliers (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, name TEXT, location TEXT, is_active BOOLEAN, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS customers (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, user_id UUID, name TEXT, phone TEXT, address TEXT, other_details TEXT, cans_held INT, balance DECIMAL, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS orders (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, customer_id UUID, qty INT, status TEXT, employee_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS employees (id UUID PRIMARY KEY, name TEXT, phone TEXT, status TEXT, role TEXT, shop_name TEXT, completed_deliveries INT, total_deliveries INT, monthly_salary DECIMAL, rating DECIMAL, today_stat INT, stat_label TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS stock_movements (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, source TEXT, qty INT, type TEXT, shop_name TEXT, employee_id UUID, product_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS sale_entries (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, date TEXT, customer_name TEXT, product_id TEXT, product_name TEXT, qty INT, purchase_price_per_unit DECIMAL, selling_price_per_unit DECIMAL, sales_margin_per_unit DECIMAL, total_selling DECIMAL, total_margin DECIMAL, shop_id TEXT, employee_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS monthly_expenses (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, month TEXT, shop_id TEXT, shop_rent DECIMAL, admin_salary DECIMAL, delivery_staff DECIMAL, miscellaneous DECIMAL, bike_expense DECIMAL, custom_expenses TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS outward (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, order_id UUID, customer_id UUID, qty_delivered INT, qty_empty_returned INT, rate DECIMAL, employee_id UUID, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS payments (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, customer_id UUID, amount DECIMAL, mode TEXT, created_at TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS route_tasks (id UUID DEFAULT uuid_generate_v4() PRIMARY KEY, customer_name TEXT, phone TEXT, address TEXT, can_qty INT, eta_text TEXT, status TEXT, employee_id UUID, order_id UUID, scheduled_date DATE, created_at TIMESTAMPTZ);

-- Migrate all tables
SELECT migrate_to_tenant_table('product_categories');
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

-- Special case: Profiles RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "profiles_isolation" ON profiles;
DROP POLICY IF EXISTS "profiles_update_self" ON profiles;

CREATE POLICY "profiles_isolation" ON profiles FOR SELECT TO authenticated
    USING (admin_id = public.get_admin_id() OR id = auth.uid());

CREATE POLICY "profiles_update_self" ON profiles FOR UPDATE TO authenticated
    USING (id = auth.uid()) WITH CHECK (id = auth.uid());

-- Cleanup unique constraint on monthly_expenses
ALTER TABLE monthly_expenses DROP CONSTRAINT IF EXISTS monthly_expenses_month_shop_id_key;
ALTER TABLE monthly_expenses DROP CONSTRAINT IF EXISTS monthly_expenses_month_shop_id_admin_id_key;
ALTER TABLE monthly_expenses ADD CONSTRAINT monthly_expenses_month_shop_id_admin_id_key UNIQUE (month, shop_id, admin_id);
