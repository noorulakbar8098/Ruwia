-- =============================================================
-- Ruwia — In-app notifications + push device tokens (tenant-scoped)
-- Run once in Supabase Dashboard → SQL Editor. Safe to re-run.
-- =============================================================

-- 1. Notification rows. Broadcast = to_user_id NULL + to_role set:
--    'employee' → every employee of the admin; 'admin' → the admin user.
CREATE TABLE IF NOT EXISTS notifications (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    admin_id UUID REFERENCES auth.users(id),
    from_user_id UUID,
    to_user_id UUID,
    to_role TEXT,
    title TEXT NOT NULL,
    body TEXT DEFAULT '',
    kind TEXT DEFAULT 'general',
    related_id TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Per-user reads. Broadcasts share one notification row; each reader
--    marks their own copy here. Deliberately NO admin_id column (one row
--    serves readers via the parent notification's tenant).
CREATE TABLE IF NOT EXISTS notification_reads (
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    read_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (notification_id, user_id)
);

-- 3. FCM device tokens for outside-the-app push.
CREATE TABLE IF NOT EXISTS device_tokens (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID NOT NULL,
    admin_id UUID REFERENCES auth.users(id),
    token TEXT NOT NULL,
    platform TEXT DEFAULT 'android',
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- A user can sign in on several Android devices. Keep one row per FCM token,
-- and upgrade installations that ran the original one-token-per-user schema.
ALTER TABLE public.device_tokens
    ADD COLUMN IF NOT EXISTS id UUID DEFAULT uuid_generate_v4();
UPDATE public.device_tokens SET id = uuid_generate_v4() WHERE id IS NULL;
ALTER TABLE public.device_tokens ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE public.device_tokens ALTER COLUMN id SET NOT NULL;

DO $$
DECLARE
    primary_key_name TEXT;
BEGIN
    SELECT conname INTO primary_key_name
    FROM pg_constraint
    WHERE conrelid = 'public.device_tokens'::regclass
      AND contype = 'p';

    IF primary_key_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.device_tokens DROP CONSTRAINT %I', primary_key_name);
    END IF;

    ALTER TABLE public.device_tokens
        ADD CONSTRAINT device_tokens_pkey PRIMARY KEY (id);
END $$;

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

-- 4. Tenant isolation via the shared helper (stamps admin_id + strict
--    admin_id = get_admin_id() policy, same as every other table).
SELECT migrate_to_tenant_table('notifications');
SELECT migrate_to_tenant_table('device_tokens');

-- 5. notification_reads has no admin_id of its own: allow a read-marker only
--    when its parent notification belongs to the caller's tenant.
ALTER TABLE notification_reads ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS notification_reads_tenant ON notification_reads;
CREATE POLICY notification_reads_tenant ON notification_reads FOR ALL TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM notifications n
        WHERE n.id = notification_reads.notification_id
          AND n.admin_id = public.get_admin_id()
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM notifications n
        WHERE n.id = notification_reads.notification_id
          AND n.admin_id = public.get_admin_id()
    )
);
