-- ============================================================
-- Migration: Grant public read access to stores
-- ============================================================
-- The store name/address/location is public reference data that the app reads
-- with the anon (publishable) key before any login exists. Tables are created
-- without RLS or grants, so the Data API roles are denied SELECT by default —
-- this migration makes public.stores readable by the anon + authenticated roles.
--
-- Pattern (Supabase-recommended): enable RLS, grant SELECT to the API roles,
-- then add an explicit read policy.
-- ============================================================

ALTER TABLE public.stores ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.stores TO anon, authenticated;

CREATE POLICY "Stores are readable by everyone"
  ON public.stores
  FOR SELECT
  TO anon, authenticated
  USING (true);
