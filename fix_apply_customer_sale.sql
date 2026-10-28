-- ============================================================
-- FIX: apply_customer_sale — allow payment to cover old balance
-- Run this in: Supabase Dashboard → SQL Editor → New Query
-- ============================================================
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
    -- Lock customer row first so we can read the live balance for validation.
    SELECT * INTO v_customer
    FROM public.customers
    WHERE id = p_customer_id AND admin_id = v_admin
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Customer not found for this business' USING ERRCODE = 'P0002';
    END IF;

    -- Payment may cover both the current invoice AND any previous outstanding
    -- balance, but cannot exceed the combined total.
    IF p_paid_now > COALESCE(v_customer.balance, 0) + p_total + 0.001 THEN
        RAISE EXCEPTION 'Payment received cannot be more than the total amount due (previous balance + this sale)';
    END IF;

    v_sale_ref    := 'sale:' || p_sale_key;
    v_payment_ref := 'sale-payment:' || p_sale_key;

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
