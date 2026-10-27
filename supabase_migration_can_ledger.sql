-- =============================================================
-- 20L customer can ledger
-- Apply after supabase_schema.sql in the Supabase SQL Editor.
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Older databases can predate columns the sale flow now writes. The can
-- ledger is only reached after the sale insert succeeds, so repair them here
-- as well (CREATE TABLE IF NOT EXISTS does not add columns to existing tables).
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS payment_received TEXT;
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS client_key TEXT;
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id);
ALTER TABLE public.stock_movements ADD COLUMN IF NOT EXISTS client_key TEXT;
ALTER TABLE public.stock_movements ADD COLUMN IF NOT EXISTS product_name TEXT;
ALTER TABLE public.stock_movements ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id);
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id);
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS cans_held INT DEFAULT 0;
UPDATE public.customers SET cans_held = 0 WHERE cans_held IS NULL;
ALTER TABLE public.customers ALTER COLUMN cans_held SET DEFAULT 0;

CREATE TABLE IF NOT EXISTS public.customer_can_ledger (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID NOT NULL REFERENCES public.customers(id) ON DELETE CASCADE,
    customer_name   TEXT NOT NULL,
    movement_type   TEXT NOT NULL CHECK (movement_type IN ('opening', 'sale', 'return', 'reversal')),
    issued_qty      INT NOT NULL DEFAULT 0 CHECK (issued_qty >= 0),
    returned_qty    INT NOT NULL DEFAULT 0 CHECK (returned_qty >= 0),
    balance_after   INT NOT NULL CHECK (balance_after >= 0),
    reference_key   TEXT NOT NULL,
    employee_id     UUID,
    shop_name       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Adds admin_id, the inject_admin_id trigger, RLS and the tenant isolation
-- policy, matching every other operational table.
SELECT public.migrate_to_tenant_table('customer_can_ledger');

-- One ledger row per logical transaction. Retried sale saves and double-tapped
-- returns reuse the same key, so a balance can never be applied twice.
CREATE UNIQUE INDEX IF NOT EXISTS customer_can_ledger_tenant_ref_idx
    ON public.customer_can_ledger (admin_id, reference_key);

CREATE INDEX IF NOT EXISTS customer_can_ledger_customer_idx
    ON public.customer_can_ledger (admin_id, customer_id, created_at DESC);

-- Atomically applies issued/returned 20L cans to a customer's balance.
-- Returns the resulting outstanding balance. Safe to retry with the same
-- reference_key: the transaction is not applied twice and the current
-- balance is returned.
CREATE OR REPLACE FUNCTION public.apply_customer_can_ledger(
    p_customer_id       UUID,
    p_issued            INT,
    p_returned          INT,
    p_reference_key     TEXT,
    p_movement_type     TEXT,
    p_employee_id       UUID DEFAULT NULL,
    p_shop_name         TEXT DEFAULT NULL,
    p_record_inward_stock BOOLEAN DEFAULT FALSE
) RETURNS INT AS $$
DECLARE
    v_admin     UUID := public.get_admin_id();
    v_customer  public.customers%ROWTYPE;
    v_new       INT;
BEGIN
    IF p_issued < 0 OR p_returned < 0 THEN
        RAISE EXCEPTION 'Issued and returned can counts must be zero or greater';
    END IF;
    IF p_reference_key IS NULL OR btrim(p_reference_key) = '' THEN
        RAISE EXCEPTION 'A ledger reference key is required';
    END IF;
    IF p_movement_type NOT IN ('opening', 'sale', 'return', 'reversal') THEN
        RAISE EXCEPTION 'Unsupported can ledger movement type: %', p_movement_type;
    END IF;

    -- Row lock serializes concurrent returns/sales for the same customer and
    -- guarantees the balance check and update see the same value.
    SELECT * INTO v_customer
    FROM public.customers
    WHERE id = p_customer_id AND admin_id = v_admin
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Customer not found for this business' USING ERRCODE = 'P0002';
    END IF;

    -- A retried transaction must not apply twice. Return the *current*
    -- balance (already locked above), not the historical balance_after from
    -- the first attempt, so callers can refresh their UI after a retry.
    IF EXISTS (
        SELECT 1
        FROM public.customer_can_ledger
        WHERE admin_id = v_admin AND reference_key = p_reference_key
    ) THEN
        RETURN COALESCE(v_customer.cans_held, 0);
    END IF;

    IF p_returned > COALESCE(v_customer.cans_held, 0) + p_issued THEN
        RAISE EXCEPTION 'Cannot return % cans: only % outstanding',
            p_returned, COALESCE(v_customer.cans_held, 0) + p_issued
            USING ERRCODE = 'P0001';
    END IF;

    v_new := COALESCE(v_customer.cans_held, 0) + p_issued - p_returned;

    UPDATE public.customers
    SET cans_held = v_new
    WHERE id = p_customer_id AND admin_id = v_admin;

    INSERT INTO public.customer_can_ledger (
        customer_id, customer_name, movement_type, issued_qty, returned_qty,
        balance_after, reference_key, employee_id, shop_name, admin_id
    ) VALUES (
        p_customer_id, COALESCE(v_customer.name, ''), p_movement_type, p_issued, p_returned,
        v_new, p_reference_key, p_employee_id, p_shop_name, v_admin
    );

    -- Standalone returns also land in the shop's Empty Cases inventory in the
    -- same transaction. Sale-time returns already wrote this movement through
    -- addOutwardSale, so the caller passes FALSE there.
    IF p_record_inward_stock AND p_returned > 0 THEN
        INSERT INTO public.stock_movements (
            source, qty, type, shop_name, employee_id, admin_id, client_key, created_at
        ) VALUES (
            'Empty cans · ' || COALESCE(v_customer.name, ''),
            p_returned,
            'inward',
            COALESCE(NULLIF(btrim(p_shop_name), ''), 'Shop 1'),
            p_employee_id,
            v_admin,
            p_reference_key,
            now()
        );
    END IF;

    RETURN v_new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Opening balances: give every existing customer with cans already held a
-- single opening ledger row so the ledger reconciles with customers.cans_held.
INSERT INTO public.customer_can_ledger (
    customer_id, customer_name, movement_type, issued_qty, returned_qty,
    balance_after, reference_key, admin_id
)
SELECT
    c.id,
    COALESCE(c.name, ''),
    'opening',
    COALESCE(c.cans_held, 0),
    0,
    COALESCE(c.cans_held, 0),
    'opening:' || c.id,
    c.admin_id
FROM public.customers c
WHERE COALESCE(c.cans_held, 0) > 0
  AND c.admin_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM public.customer_can_ledger l
      WHERE l.admin_id = c.admin_id
        AND l.reference_key = 'opening:' || c.id
  );

-- Refresh PostgREST's schema cache so the new columns and RPC are visible
-- immediately (otherwise the app can still see "column/function not found").
NOTIFY pgrst, 'reload schema';
