-- =============================================================
-- Customer money account (sales, payments, outstanding balance)
-- Apply after supabase_schema.sql and supabase_migration_can_ledger.sql
-- in the Supabase SQL Editor.
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Older databases predate some columns the app already writes.
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS payment_received TEXT;
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS client_key TEXT;
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS customer_id UUID REFERENCES public.customers(id) ON DELETE SET NULL;
ALTER TABLE public.sale_entries ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id);

ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id);
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS sale_client_key TEXT;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS reference_key TEXT;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS note TEXT;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS shop_name TEXT;

SELECT public.migrate_to_tenant_table('payments');

CREATE INDEX IF NOT EXISTS sale_entries_customer_idx
    ON public.sale_entries (admin_id, customer_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS payments_tenant_ref_idx
    ON public.payments (admin_id, reference_key)
    WHERE reference_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.customer_account_ledger (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID NOT NULL REFERENCES public.customers(id) ON DELETE CASCADE,
    customer_name   TEXT NOT NULL DEFAULT '',
    entry_type      TEXT NOT NULL CHECK (entry_type IN ('opening', 'sale', 'payment', 'adjustment', 'reversal')),
    sale_client_key TEXT,
    payment_id      UUID REFERENCES public.payments(id) ON DELETE SET NULL,
    debit           NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (debit >= 0),
    credit          NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (credit >= 0),
    balance_after   NUMERIC(12, 2) NOT NULL,
    reference_key   TEXT NOT NULL,
    note            TEXT,
    mode            TEXT,
    employee_id     UUID,
    shop_name       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    admin_id        UUID
);

-- Adds admin_id, the inject_admin_id trigger, RLS and the tenant isolation
-- policy, matching every other operational table.
SELECT public.migrate_to_tenant_table('customer_account_ledger');

CREATE UNIQUE INDEX IF NOT EXISTS customer_account_ledger_tenant_ref_idx
    ON public.customer_account_ledger (admin_id, reference_key);

CREATE INDEX IF NOT EXISTS customer_account_ledger_customer_idx
    ON public.customer_account_ledger (admin_id, customer_id, created_at DESC);

-- Atomically records a sale (debit) and the optional payment taken at sale
-- time (credit). The customer row is locked so concurrent sales/payments for
-- the same account cannot interleave. Retrying with the same sale key returns
-- the current balance without applying anything twice.
CREATE OR REPLACE FUNCTION public.apply_customer_sale(
    p_customer_id   UUID,
    p_sale_key      TEXT,
    p_total         NUMERIC,
    p_paid_now      NUMERIC DEFAULT 0,
    p_payment_mode  TEXT DEFAULT 'cash',
    p_note          TEXT DEFAULT NULL,
    p_employee_id   UUID DEFAULT NULL,
    p_shop_name     TEXT DEFAULT NULL
) RETURNS NUMERIC AS $$
DECLARE
    v_admin         UUID := public.get_admin_id();
    v_customer      public.customers%ROWTYPE;
    v_previous      NUMERIC;
    v_after_sale    NUMERIC;
    v_after_payment NUMERIC;
    v_payment_id    UUID;
    v_sale_ref      TEXT;
    v_payment_ref   TEXT;
BEGIN
    IF p_sale_key IS NULL OR btrim(p_sale_key) = '' THEN
        RAISE EXCEPTION 'A sale key is required';
    END IF;
    IF p_total < 0 OR p_paid_now < 0 THEN
        RAISE EXCEPTION 'Sale total and payment cannot be negative';
    END IF;
    IF p_paid_now > p_total + 0.001 THEN
        RAISE EXCEPTION 'Payment received cannot be more than the sale total';
    END IF;

    v_sale_ref    := 'sale:' || p_sale_key;
    v_payment_ref := 'sale-payment:' || p_sale_key;

    SELECT * INTO v_customer
    FROM public.customers
    WHERE id = p_customer_id AND admin_id = v_admin
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Customer not found for this business' USING ERRCODE = 'P0002';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.customer_account_ledger
        WHERE admin_id = v_admin AND reference_key = v_sale_ref
    ) THEN
        RETURN COALESCE(v_customer.balance, 0);
    END IF;

    v_previous   := COALESCE(v_customer.balance, 0);
    v_after_sale := v_previous + p_total;

    INSERT INTO public.customer_account_ledger (
        customer_id, customer_name, entry_type, sale_client_key,
        debit, credit, balance_after, reference_key, note,
        employee_id, shop_name, admin_id, created_at
    ) VALUES (
        p_customer_id, COALESCE(v_customer.name, ''), 'sale', p_sale_key,
        p_total, 0, v_after_sale, v_sale_ref, p_note,
        p_employee_id, p_shop_name, v_admin, clock_timestamp()
    );

    IF p_paid_now > 0 THEN
        INSERT INTO public.payments (
            customer_id, amount, mode, sale_client_key, reference_key, note,
            created_by, shop_name, admin_id, created_at
        ) VALUES (
            p_customer_id, p_paid_now,
            COALESCE(NULLIF(btrim(p_payment_mode), ''), 'cash'),
            p_sale_key, v_payment_ref, p_note,
            p_employee_id, p_shop_name, v_admin,
            clock_timestamp() + interval '1 microsecond'
        ) RETURNING id INTO v_payment_id;

        v_after_payment := v_after_sale - p_paid_now;

        INSERT INTO public.customer_account_ledger (
            customer_id, customer_name, entry_type, sale_client_key, payment_id,
            debit, credit, balance_after, reference_key, note, mode,
            employee_id, shop_name, admin_id, created_at
        ) VALUES (
            p_customer_id, COALESCE(v_customer.name, ''), 'payment', p_sale_key, v_payment_id,
            0, p_paid_now, v_after_payment, v_payment_ref, p_note,
            COALESCE(NULLIF(btrim(p_payment_mode), ''), 'cash'),
            p_employee_id, p_shop_name, v_admin,
            clock_timestamp() + interval '2 microseconds'
        );

        v_after_sale := v_after_payment;
    END IF;

    UPDATE public.customers
    SET balance = v_after_sale
    WHERE id = p_customer_id AND admin_id = v_admin;

    RETURN v_after_sale;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Atomically records a standalone payment (credit) against an outstanding
-- balance. The reference key makes retries and double taps idempotent.
CREATE OR REPLACE FUNCTION public.apply_customer_payment(
    p_customer_id    UUID,
    p_amount         NUMERIC,
    p_payment_mode   TEXT DEFAULT 'cash',
    p_reference_key  TEXT DEFAULT NULL,
    p_sale_client_key TEXT DEFAULT NULL,
    p_note           TEXT DEFAULT NULL,
    p_employee_id    UUID DEFAULT NULL,
    p_shop_name      TEXT DEFAULT NULL
) RETURNS NUMERIC AS $$
DECLARE
    v_admin       UUID := public.get_admin_id();
    v_customer    public.customers%ROWTYPE;
    v_previous    NUMERIC;
    v_new         NUMERIC;
    v_payment_id  UUID;
    v_payment_ref TEXT;
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Payment amount must be greater than zero';
    END IF;
    IF p_reference_key IS NULL OR btrim(p_reference_key) = '' THEN
        RAISE EXCEPTION 'A payment reference key is required';
    END IF;

    v_payment_ref := 'payment:' || p_reference_key;

    SELECT * INTO v_customer
    FROM public.customers
    WHERE id = p_customer_id AND admin_id = v_admin
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Customer not found for this business' USING ERRCODE = 'P0002';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.customer_account_ledger
        WHERE admin_id = v_admin AND reference_key = v_payment_ref
    ) THEN
        RETURN COALESCE(v_customer.balance, 0);
    END IF;

    v_previous := COALESCE(v_customer.balance, 0);
    IF p_amount > v_previous + 0.001 THEN
        RAISE EXCEPTION 'Payment of % exceeds outstanding balance of %', p_amount, v_previous;
    END IF;
    v_new := v_previous - p_amount;

    INSERT INTO public.payments (
        customer_id, amount, mode, sale_client_key, reference_key, note,
        created_by, shop_name, admin_id, created_at
    ) VALUES (
        p_customer_id, p_amount,
        COALESCE(NULLIF(btrim(p_payment_mode), ''), 'cash'),
        NULLIF(btrim(p_sale_client_key), ''), v_payment_ref, p_note,
        p_employee_id, p_shop_name, v_admin, clock_timestamp()
    ) RETURNING id INTO v_payment_id;

    INSERT INTO public.customer_account_ledger (
        customer_id, customer_name, entry_type, sale_client_key, payment_id,
        debit, credit, balance_after, reference_key, note, mode,
        employee_id, shop_name, admin_id, created_at
    ) VALUES (
        p_customer_id, COALESCE(v_customer.name, ''), 'payment',
        NULLIF(btrim(p_sale_client_key), ''), v_payment_id,
        0, p_amount, v_new, v_payment_ref, p_note,
        COALESCE(NULLIF(btrim(p_payment_mode), ''), 'cash'),
        p_employee_id, p_shop_name, v_admin, clock_timestamp()
    );

    UPDATE public.customers
    SET balance = v_new
    WHERE id = p_customer_id AND admin_id = v_admin;

    RETURN v_new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── Legacy backfill ──────────────────────────────────────────────────────────
-- Link historical sales to customers by name where the customer id was never
-- stored. Ambiguous duplicate names are resolved to one customer per tenant;
-- the admin can correct a mis-linked row later.
UPDATE public.sale_entries se
SET customer_id = c.id
FROM public.customers c
WHERE se.customer_id IS NULL
  AND se.admin_id = c.admin_id
  AND lower(btrim(se.customer_name)) = lower(btrim(c.name));

-- One payment row per logical sale (client_key), using max(payment_received)
-- because the old app repeated the same amount on every line item.
WITH grouped AS (
    SELECT
        se.admin_id,
        se.customer_id,
        COALESCE(NULLIF(btrim(se.client_key), ''), 'legacy:' || se.id::text) AS sale_key,
        MIN(se.created_at) AS created_at,
        SUM(COALESCE(se.total_selling, se.qty * se.selling_price_per_unit, 0)) AS total,
        LEAST(
            COALESCE(MAX(
                CASE
                    WHEN regexp_replace(COALESCE(se.payment_received, ''), '[^0-9.]', '', 'g') ~ '^[0-9]+(\.[0-9]+)?$'
                    THEN regexp_replace(COALESCE(se.payment_received, ''), '[^0-9.]', '', 'g')::NUMERIC
                    ELSE 0
                END
            ), 0),
            SUM(COALESCE(se.total_selling, se.qty * se.selling_price_per_unit, 0))
        ) AS paid
    FROM public.sale_entries se
    WHERE se.customer_id IS NOT NULL
      AND se.admin_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM public.customer_account_ledger l
          WHERE l.admin_id = se.admin_id
            AND l.reference_key = 'sale:' || COALESCE(NULLIF(btrim(se.client_key), ''), 'legacy:' || se.id::text)
      )
    GROUP BY
        se.admin_id,
        se.customer_id,
        COALESCE(NULLIF(btrim(se.client_key), ''), 'legacy:' || se.id::text)
)
INSERT INTO public.payments (
    customer_id, amount, mode, sale_client_key, reference_key, note,
    admin_id, created_at
)
SELECT
    g.customer_id,
    g.paid,
    'cash',
    g.sale_key,
    'legacy-sale-payment:' || g.sale_key,
    'Backfilled from sale_entries.payment_received',
    g.admin_id,
    COALESCE(g.created_at, now())
FROM grouped g
WHERE g.paid > 0
  AND NOT EXISTS (
      SELECT 1
      FROM public.payments p
      WHERE p.admin_id = g.admin_id
        AND p.reference_key = 'legacy-sale-payment:' || g.sale_key
  );

-- Ledger rows for the same history. balance_after is computed over the whole
-- history, so re-running the migration is safe.
WITH grouped AS (
    SELECT
        se.admin_id,
        se.customer_id,
        MAX(se.customer_name) AS customer_name,
        COALESCE(NULLIF(btrim(se.client_key), ''), 'legacy:' || se.id::text) AS sale_key,
        MIN(se.created_at) AS created_at,
        SUM(COALESCE(se.total_selling, se.qty * se.selling_price_per_unit, 0)) AS total,
        LEAST(
            COALESCE(MAX(
                CASE
                    WHEN regexp_replace(COALESCE(se.payment_received, ''), '[^0-9.]', '', 'g') ~ '^[0-9]+(\.[0-9]+)?$'
                    THEN regexp_replace(COALESCE(se.payment_received, ''), '[^0-9.]', '', 'g')::NUMERIC
                    ELSE 0
                END
            ), 0),
            SUM(COALESCE(se.total_selling, se.qty * se.selling_price_per_unit, 0))
        ) AS paid
    FROM public.sale_entries se
    WHERE se.customer_id IS NOT NULL
      AND se.admin_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM public.customer_account_ledger l
          WHERE l.admin_id = se.admin_id
            AND l.reference_key = 'sale:' || COALESCE(NULLIF(btrim(se.client_key), ''), 'legacy:' || se.id::text)
      )
    GROUP BY
        se.admin_id,
        se.customer_id,
        COALESCE(NULLIF(btrim(se.client_key), ''), 'legacy:' || se.id::text)
),
entries AS (
    SELECT
        g.admin_id,
        g.customer_id,
        COALESCE(g.customer_name, '') AS customer_name,
        'sale'::TEXT AS entry_type,
        g.sale_key AS sale_client_key,
        NULL::UUID AS payment_id,
        g.total AS debit,
        0::NUMERIC AS credit,
        'legacy-sale:' || g.sale_key AS reference_key,
        'Backfilled sale'::TEXT AS note,
        NULL::TEXT AS mode,
        COALESCE(g.created_at, now()) AS created_at,
        0 AS sort_order
    FROM grouped g
    UNION ALL
    SELECT
        g.admin_id,
        g.customer_id,
        COALESCE(g.customer_name, ''),
        'payment',
        g.sale_key,
        p.id,
        0,
        g.paid,
        'legacy-sale-payment:' || g.sale_key,
        'Backfilled payment',
        'cash',
        COALESCE(g.created_at, now()) + interval '1 microsecond',
        1
    FROM grouped g
    JOIN public.payments p
      ON p.admin_id = g.admin_id
     AND p.reference_key = 'legacy-sale-payment:' || g.sale_key
    WHERE g.paid > 0
),
running AS (
    SELECT
        e.*,
        SUM(e.debit - e.credit) OVER (
            PARTITION BY e.admin_id, e.customer_id
            ORDER BY e.created_at, e.sort_order, e.reference_key
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS balance_after
    FROM entries e
)
INSERT INTO public.customer_account_ledger (
    customer_id, customer_name, entry_type, sale_client_key, payment_id,
    debit, credit, balance_after, reference_key, note, mode,
    created_at, admin_id
)
SELECT
    r.customer_id, r.customer_name, r.entry_type, r.sale_client_key, r.payment_id,
    r.debit, r.credit, r.balance_after, r.reference_key, r.note, r.mode,
    r.created_at, r.admin_id
FROM running r
WHERE NOT EXISTS (
    SELECT 1
    FROM public.customer_account_ledger l
    WHERE l.admin_id = r.admin_id
      AND l.reference_key = r.reference_key
);

-- Customers with a manually maintained balance but no sale history get one
-- opening entry, so their ledger detail matches the amount shown in the
-- customer list instead of reading zero.
INSERT INTO public.customer_account_ledger (
    customer_id, customer_name, entry_type, debit, credit, balance_after,
    reference_key, note, admin_id
)
SELECT
    c.id,
    COALESCE(c.name, ''),
    'opening',
    c.balance,
    0,
    c.balance,
    'opening:' || c.id,
    'Opening balance migrated from customers.balance',
    c.admin_id
FROM public.customers c
WHERE c.admin_id IS NOT NULL
  AND COALESCE(c.balance, 0) > 0
  AND NOT EXISTS (
      SELECT 1
      FROM public.customer_account_ledger l
      WHERE l.admin_id = c.admin_id
        AND l.customer_id = c.id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM public.customer_account_ledger l
      WHERE l.admin_id = c.admin_id
        AND l.reference_key = 'opening:' || c.id
  );

-- Make the cached customers.balance agree with the ledger. Customers without
-- any ledger history keep their existing balance untouched.
UPDATE public.customers c
SET balance = x.outstanding
FROM (
    SELECT admin_id, customer_id, SUM(debit - credit) AS outstanding
    FROM public.customer_account_ledger
    GROUP BY admin_id, customer_id
) x
WHERE c.admin_id = x.admin_id
  AND c.id = x.customer_id;

NOTIFY pgrst, 'reload schema';
