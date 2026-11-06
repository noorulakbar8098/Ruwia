-- =============================================================
-- Ruwia — Recipient-scoped RLS for notifications + device tokens
-- Run once in Supabase Dashboard → SQL Editor. Safe to re-run.
--
-- supabase_migration_notifications.sql used migrate_to_tenant_table(),
-- which grants every member of a tenant full read/write on every row.
-- That lets an employee read other employees' direct notifications and
-- FCM tokens through PostgREST. This migration replaces those tenant-wide
-- policies with recipient/user-scoped ones.
-- =============================================================

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_reads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_tokens ENABLE ROW LEVEL SECURITY;

-- Drop the tenant-wide policies created by migrate_to_tenant_table() and
-- the previous notification_reads policy.
DROP POLICY IF EXISTS notifications_isolation ON public.notifications;
DROP POLICY IF EXISTS notifications_rw ON public.notifications;
DROP POLICY IF EXISTS notifications_multi_tenant ON public.notifications;
DROP POLICY IF EXISTS notifications_read ON public.notifications;
DROP POLICY IF EXISTS notifications_admin ON public.notifications;

DROP POLICY IF EXISTS device_tokens_isolation ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_rw ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_multi_tenant ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_read ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_admin ON public.device_tokens;

DROP POLICY IF EXISTS notification_reads_tenant ON public.notification_reads;

-- Also replace this migration's own policies so a re-run is idempotent.
DROP POLICY IF EXISTS notifications_select_visible ON public.notifications;
DROP POLICY IF EXISTS notifications_insert_sender ON public.notifications;
DROP POLICY IF EXISTS notification_reads_select_own ON public.notification_reads;
DROP POLICY IF EXISTS notification_reads_insert_own ON public.notification_reads;
DROP POLICY IF EXISTS notification_reads_delete_own ON public.notification_reads;
DROP POLICY IF EXISTS device_tokens_select_own ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_insert_own ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_update_own ON public.device_tokens;
DROP POLICY IF EXISTS device_tokens_delete_own ON public.device_tokens;

-- ─── notifications ──────────────────────────────────────────
-- Visible when the caller sent it, it is addressed to them directly, or it
-- is a role broadcast for their role in this tenant.
CREATE POLICY notifications_select_visible
ON public.notifications
FOR SELECT
TO authenticated
USING (
    admin_id = public.get_admin_id()
    AND (
        from_user_id = auth.uid()
        OR to_user_id = auth.uid()
        OR (
            to_user_id IS NULL
            AND (
                (
                    to_role = 'admin'
                    AND auth.uid() = public.get_admin_id()
                )
                OR (
                    to_role = 'employee'
                    AND EXISTS (
                        SELECT 1
                        FROM public.employees e
                        WHERE e.id = auth.uid()
                          AND e.admin_id = notifications.admin_id
                          AND e.status = 'active'
                    )
                )
            )
        )
    )
);

-- The sender must be the caller. Admins may broadcast or target an active
-- employee in their own tenant; employees may only message their admin.
-- No UPDATE/DELETE policies: the feed is append-only from the app.
CREATE POLICY notifications_insert_sender
ON public.notifications
FOR INSERT
TO authenticated
WITH CHECK (
    admin_id = public.get_admin_id()
    AND from_user_id = auth.uid()
    AND (
        (
            auth.uid() = public.get_admin_id()
            AND (
                to_user_id IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM public.employees e
                    WHERE e.id = notifications.to_user_id
                      AND e.admin_id = notifications.admin_id
                      AND e.status = 'active'
                )
            )
        )
        OR (
            to_user_id IS NULL
            AND to_role = 'admin'
        )
    )
);

-- ─── notification_reads ─────────────────────────────────────
-- A marker belongs to exactly one user and only for a notification they can
-- already see (the notifications SELECT policy filters the subquery).
CREATE POLICY notification_reads_select_own
ON public.notification_reads
FOR SELECT
TO authenticated
USING (
    user_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.notifications n
        WHERE n.id = notification_reads.notification_id
    )
);

CREATE POLICY notification_reads_insert_own
ON public.notification_reads
FOR INSERT
TO authenticated
WITH CHECK (
    user_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.notifications n
        WHERE n.id = notification_reads.notification_id
    )
);

CREATE POLICY notification_reads_delete_own
ON public.notification_reads
FOR DELETE
TO authenticated
USING (user_id = auth.uid());

-- ─── device_tokens ──────────────────────────────────────────
-- Tokens are private to their owner. The send-push edge function uses the
-- service role, which bypasses RLS, to fan out to other users.
CREATE POLICY device_tokens_select_own
ON public.device_tokens
FOR SELECT
TO authenticated
USING (
    user_id = auth.uid()
    AND admin_id = public.get_admin_id()
);

CREATE POLICY device_tokens_insert_own
ON public.device_tokens
FOR INSERT
TO authenticated
WITH CHECK (
    user_id = auth.uid()
    AND admin_id = public.get_admin_id()
);

CREATE POLICY device_tokens_update_own
ON public.device_tokens
FOR UPDATE
TO authenticated
USING (
    user_id = auth.uid()
    AND admin_id = public.get_admin_id()
)
WITH CHECK (
    user_id = auth.uid()
    AND admin_id = public.get_admin_id()
);

CREATE POLICY device_tokens_delete_own
ON public.device_tokens
FOR DELETE
TO authenticated
USING (
    user_id = auth.uid()
    AND admin_id = public.get_admin_id()
);

-- Guarantee the conflict target used by the RPC below. The original
-- notifications migration already adds this; repeated here so this file can
-- be applied on its own.
WITH duplicate_tokens AS (
    SELECT ctid,
           row_number() OVER (
               PARTITION BY token
               ORDER BY updated_at DESC NULLS LAST, ctid DESC
           ) AS row_number
    FROM public.device_tokens
)
DELETE FROM public.device_tokens
WHERE ctid IN (SELECT ctid FROM duplicate_tokens WHERE row_number > 1);

CREATE UNIQUE INDEX IF NOT EXISTS device_tokens_token_unique
    ON public.device_tokens(token);

-- ─── Token upsert RPC ───────────────────────────────────────
-- Registration must reassign a known token to the caller (signing into a
-- second account on the same device) without letting a client choose
-- another user's id. SECURITY DEFINER keeps that atomic and lets the
-- device_tokens UPDATE policy stay strictly self-scoped.
CREATE OR REPLACE FUNCTION public.upsert_device_token(
    p_token TEXT,
    p_platform TEXT DEFAULT 'android'
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_admin_id UUID;
    v_token TEXT := btrim(COALESCE(p_token, ''));
    v_platform TEXT := COALESCE(NULLIF(btrim(p_platform), ''), 'android');
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'not authenticated' USING ERRCODE = '28000';
    END IF;
    IF v_token = '' THEN
        RAISE EXCEPTION 'device token is required' USING ERRCODE = '22023';
    END IF;

    v_admin_id := public.get_admin_id();
    IF v_admin_id IS NULL THEN
        RAISE EXCEPTION 'cannot resolve tenant for user %', v_user_id USING ERRCODE = '22023';
    END IF;

    INSERT INTO public.device_tokens (user_id, admin_id, token, platform, updated_at)
    VALUES (v_user_id, v_admin_id, v_token, v_platform, now())
    ON CONFLICT (token) DO UPDATE
        SET user_id = EXCLUDED.user_id,
            admin_id = EXCLUDED.admin_id,
            platform = EXCLUDED.platform,
            updated_at = now();
END;
$$;

REVOKE ALL ON FUNCTION public.upsert_device_token(TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.upsert_device_token(TEXT, TEXT) TO authenticated;
