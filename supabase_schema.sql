-- =============================================================
-- Ruwia / Neer Thuli — Supabase database schema  (COMPLETE)
-- Run this in Supabase SQL Editor (Project → SQL Editor → New query → Run)
--
-- Safe to re-run: every statement is idempotent
-- (IF NOT EXISTS / OR REPLACE / DROP POLICY IF EXISTS).
-- Tables are created in foreign-key dependency order.
-- =============================================================

-- ─── Extensions ──────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── Profiles (1 row per auth user; drives role / admin checks) ─
CREATE TABLE IF NOT EXISTS profiles (
    id         UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    role       TEXT NOT NULL DEFAULT 'user',   -- 'admin' | 'user' | 'employee'
    full_name  TEXT,
    phone      TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Auto-create a profile row whenever a new auth user signs up.
-- Reads full_name / phone from the sign-up metadata (see AuthRepository.signUp).
-- The VERY FIRST account to sign up becomes 'admin' automatically; everyone
-- after that defaults to 'user'. No manual email/UUID promotion needed.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    assigned_role TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.profiles WHERE role = 'admin') THEN
        assigned_role := 'admin';     -- first ever account → admin
    ELSE
        assigned_role := 'user';      -- everyone else → normal user
    END IF;

    INSERT INTO public.profiles (id, role, full_name, phone)
    VALUES (
        NEW.id,
        assigned_role,
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'phone'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Backfill profiles for any users that already exist.
INSERT INTO public.profiles (id, role, full_name, phone)
SELECT id, 'user', raw_user_meta_data->>'full_name', raw_user_meta_data->>'phone'
FROM auth.users
ON CONFLICT (id) DO NOTHING;

-- Bootstrap: if no admin exists yet (e.g. you already signed up before running
-- this), promote the earliest-registered account to admin. Fully dynamic —
-- no email or UUID to type.
UPDATE public.profiles
SET role = 'admin'
WHERE id = (SELECT id FROM auth.users ORDER BY created_at ASC LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM public.profiles WHERE role = 'admin');

-- ─── Product Categories ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_categories (
    id               UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name             TEXT NOT NULL,
    display_name     TEXT NOT NULL,
    supplier_group   TEXT NOT NULL DEFAULT 'GC',
    purchase_price_gc  DECIMAL(10,2) DEFAULT 0,
    purchase_price_mb  DECIMAL(10,2) DEFAULT 0,
    default_sell_price DECIMAL(10,2) DEFAULT 0,
    stock_available  INT DEFAULT 0,
    is_active        BOOLEAN DEFAULT true,
    created_at       TIMESTAMPTZ DEFAULT now()
);

-- ─── Shop Stocks ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS shop_stocks (
    id                   UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name                 TEXT NOT NULL,
    location             TEXT NOT NULL,
    total_cans           INT DEFAULT 0,
    full_cans            INT DEFAULT 0,
    empty_cans           INT DEFAULT 0,
    cans_with_customers  INT DEFAULT 0,
    is_live              BOOLEAN DEFAULT true,
    updated_at           TIMESTAMPTZ DEFAULT now()
);

-- ─── Suppliers ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS suppliers (
    id         UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name       TEXT NOT NULL,
    location   TEXT,
    is_active  BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ─── Customers ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    id            UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id       UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    name          TEXT NOT NULL,
    phone         TEXT,
    address       TEXT,
    other_details TEXT,
    cans_held     INT DEFAULT 0,
    balance       DECIMAL(10,2) DEFAULT 0,
    created_at    TIMESTAMPTZ DEFAULT now()
);

-- ─── Orders ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id          UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    qty         INT NOT NULL DEFAULT 1,
    status      TEXT DEFAULT 'pending',
    employee_id UUID REFERENCES profiles(id),
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ─── Employees ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
    id                   UUID REFERENCES profiles(id) ON DELETE CASCADE PRIMARY KEY,
    name                 TEXT NOT NULL,
    phone                TEXT,
    status               TEXT DEFAULT 'active',
    role                 TEXT DEFAULT 'staff',
    shop_name            TEXT DEFAULT 'Shop 1',
    completed_deliveries INT DEFAULT 0,
    total_deliveries     INT DEFAULT 0,
    monthly_salary       DECIMAL(10,2) DEFAULT 0,
    rating               DECIMAL(3,1) DEFAULT 5.0,
    today_stat           INT DEFAULT 0,
    stat_label           TEXT DEFAULT 'TODAY',
    created_at           TIMESTAMPTZ DEFAULT now()
);

-- ─── Stock Movements ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_movements (
    id          UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    source      TEXT NOT NULL,
    qty         INT NOT NULL,
    type        TEXT NOT NULL,
    shop_name   TEXT NOT NULL DEFAULT 'Shop 1',
    employee_id UUID REFERENCES profiles(id),
    product_id  UUID REFERENCES product_categories(id),
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ─── Sale Entries ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sale_entries (
    id                       UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    date                     TEXT NOT NULL,
    customer_name            TEXT NOT NULL,
    product_id               TEXT NOT NULL,
    product_name             TEXT NOT NULL,
    qty                      INT NOT NULL,
    purchase_price_per_unit  DECIMAL(10,2) DEFAULT 0,
    selling_price_per_unit   DECIMAL(10,2) DEFAULT 0,
    sales_margin_per_unit    DECIMAL(10,2) DEFAULT 0,
    total_selling            DECIMAL(10,2) DEFAULT 0,
    total_margin             DECIMAL(10,2) DEFAULT 0,
    shop_id                  TEXT DEFAULT 'shop1',
    employee_id              UUID REFERENCES profiles(id),
    created_at               TIMESTAMPTZ DEFAULT now()
);

-- ─── Monthly Expenses ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS monthly_expenses (
    id              UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    month           TEXT NOT NULL,
    shop_id         TEXT DEFAULT 'shop1',
    shop_rent       DECIMAL(10,2) DEFAULT 0,
    admin_salary    DECIMAL(10,2) DEFAULT 0,
    delivery_staff  DECIMAL(10,2) DEFAULT 0,
    miscellaneous   DECIMAL(10,2) DEFAULT 0,
    bike_expense    DECIMAL(10,2) DEFAULT 0,
    custom_expenses TEXT DEFAULT NULL,
    created_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (month, shop_id)
);

-- ─── Outward (deliveries to customers) ────────────────────────
CREATE TABLE IF NOT EXISTS outward (
    id                 UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    order_id           UUID REFERENCES orders(id) ON DELETE SET NULL,
    customer_id        UUID REFERENCES customers(id) ON DELETE CASCADE,
    qty_delivered      INT NOT NULL DEFAULT 0,
    qty_empty_returned INT DEFAULT 0,
    rate               DECIMAL(10,2) DEFAULT 0,
    employee_id        UUID REFERENCES profiles(id),
    created_at         TIMESTAMPTZ DEFAULT now()
);

-- ─── Payments ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id          UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
    mode        TEXT DEFAULT 'cash',
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ─── Route Tasks (delivery assignments) ───────────────────────
CREATE TABLE IF NOT EXISTS route_tasks (
    id             UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    customer_name  TEXT NOT NULL,
    phone          TEXT,
    address        TEXT,
    can_qty        INT NOT NULL DEFAULT 1,
    eta_text       TEXT DEFAULT '',
    status         TEXT DEFAULT 'queued',
    employee_id    UUID REFERENCES profiles(id),
    order_id       UUID REFERENCES orders(id),
    scheduled_date DATE DEFAULT CURRENT_DATE,
    created_at     TIMESTAMPTZ DEFAULT now()
);

-- =============================================================
-- Seed data
-- =============================================================

INSERT INTO product_categories (name, display_name, supplier_group, purchase_price_gc, purchase_price_mb, default_sell_price, stock_available)
VALUES
    ('300ML',    '300ML Water Can',   'GC', 127.0, 127.0, 140.0, 0),
    ('500ML',    '500ML Water Can',   'GC', 127.0, 127.0, 140.0, 0),
    ('1 Litre',  '1 Litre Water Can', 'GC',  88.0,  88.0, 108.0, 0),
    ('2 Litre',  '2 Litre Water Can', 'MB', 122.0, 122.0, 140.0, 0),
    ('5 Litre',  '5 Litre Water Can', 'GC',   0.0,   0.0,   0.0, 0),
    ('20 Litre', '20L Water Can',     'GC',  16.5,  16.5,  30.0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO shop_stocks (name, location, total_cans, full_cans, empty_cans, cans_with_customers)
VALUES
    ('Shop 1 · Main',   'SAIBABA COLONY', 0, 0, 0, 0),
    ('Shop 2 · Branch', 'RS PURAM',       0, 0, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO suppliers (name, location)
VALUES
    ('Global Creators', 'Tiru'),
    ('Multi Brands', 'Coimbatore'),
    ('Aqua Pure Plant', 'Tiru')
ON CONFLICT DO NOTHING;

-- =============================================================
-- Row Level Security
-- =============================================================
-- NOTE: These are DEVELOPMENT policies — any authenticated user can read and
-- write the operational tables, so the app works immediately after login.
-- For production, replace the "_write" policies on product_categories,
-- suppliers, shop_stocks and monthly_expenses with the admin-gated versions
-- shown at the bottom of this file, and promote your account to admin.

ALTER TABLE profiles            ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_categories  ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_stocks         ENABLE ROW LEVEL SECURITY;
ALTER TABLE suppliers           ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers           ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders              ENABLE ROW LEVEL SECURITY;
ALTER TABLE employees           ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements     ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_entries        ENABLE ROW LEVEL SECURITY;
ALTER TABLE monthly_expenses    ENABLE ROW LEVEL SECURITY;
ALTER TABLE outward             ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments            ENABLE ROW LEVEL SECURITY;
ALTER TABLE route_tasks         ENABLE ROW LEVEL SECURITY;

-- profiles: everyone authenticated can read (needed for role checks); user edits own row.
DROP POLICY IF EXISTS "profiles_read"   ON profiles;
DROP POLICY IF EXISTS "profiles_update" ON profiles;
CREATE POLICY "profiles_read"   ON profiles FOR SELECT TO authenticated USING (true);
CREATE POLICY "profiles_update" ON profiles FOR UPDATE TO authenticated
    USING (id = auth.uid()) WITH CHECK (id = auth.uid());

-- Helper macro: read + full write for any authenticated user.
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'product_categories','shop_stocks','suppliers','customers','orders',
        'employees','stock_movements','sale_entries','monthly_expenses',
        'outward','payments','route_tasks'
    ] LOOP
        EXECUTE format('DROP POLICY IF EXISTS "%s_rw" ON %I;', t, t);
        EXECUTE format(
            'CREATE POLICY "%s_rw" ON %I FOR ALL TO authenticated USING (true) WITH CHECK (true);',
            t, t
        );
    END LOOP;
END $$;

-- =============================================================
-- PRODUCTION HARDENING (optional — run later)
-- =============================================================
-- 1) Promote your logged-in account to admin (replace the email):
--      UPDATE profiles SET role = 'admin'
--      WHERE id = (SELECT id FROM auth.users WHERE email = 'you@example.com');
--
-- 2) Replace the permissive "_rw" policies on admin-only tables with:
--      DROP POLICY IF EXISTS "product_categories_rw" ON product_categories;
--      CREATE POLICY "product_categories_read"  ON product_categories FOR SELECT TO authenticated USING (true);
--      CREATE POLICY "product_categories_admin" ON product_categories FOR ALL    TO authenticated
--        USING      (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'))
--        WITH CHECK (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));
--    (repeat for suppliers, shop_stocks, monthly_expenses)
--
-- 3) Migration for existing installations (custom monthly expenses):
--      ALTER TABLE monthly_expenses ADD COLUMN IF NOT EXISTS custom_expenses TEXT DEFAULT NULL;
