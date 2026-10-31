-- =============================================================
-- Admin delivery tasks (customer + product + schedule + assignee)
-- Apply after supabase_schema.sql in the Supabase SQL Editor.
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Newer builds store the structured delivery details on route_tasks. Older
-- databases only have the original customer_name/phone/address/can_qty
-- columns, so repair them before the admin task form writes to them.
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS customer_address TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS employee_name TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS product_id UUID;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS product_name TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS delivery_date DATE;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES auth.users(id);
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS timeline JSONB DEFAULT '[]'::jsonb;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS dest_latitude DOUBLE PRECISION;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS dest_longitude DOUBLE PRECISION;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS dest_name TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS maps_link TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS current_latitude DOUBLE PRECISION;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS current_longitude DOUBLE PRECISION;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS current_location_name TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS journey_start_time TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS last_location_update_time TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS arrival_time TEXT;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS distance_remaining_meters DOUBLE PRECISION;
ALTER TABLE public.route_tasks ADD COLUMN IF NOT EXISTS live_eta TEXT;

ALTER TABLE public.route_tasks ALTER COLUMN status SET DEFAULT 'pending';

-- The admin board reads the tenant's schedule and the employee app reads one
-- employee's schedule for today.
CREATE INDEX IF NOT EXISTS route_tasks_admin_schedule_idx
    ON public.route_tasks (admin_id, scheduled_date);

CREATE INDEX IF NOT EXISTS route_tasks_employee_schedule_idx
    ON public.route_tasks (admin_id, employee_id, scheduled_date);

-- Keep the scheduled date in sync for rows written by older clients that only
-- knew delivery_date.
UPDATE public.route_tasks
SET scheduled_date = delivery_date
WHERE scheduled_date IS NULL
  AND delivery_date IS NOT NULL;

-- Refresh PostgREST's schema cache so the new columns are visible immediately.
NOTIFY pgrst, 'reload schema';
