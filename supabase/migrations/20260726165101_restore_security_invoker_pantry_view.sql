-- ============================================================
-- Migration: restore security_invoker on pantry_items_by_expire
-- ============================================================
-- Migration 20260725015736 dropped and recreated this view to add the
-- expires_at column. DROP + CREATE resets a view's options to their defaults,
-- so it lost the security_invoker = true set in 20260718223526. Apply it
-- here so the view executes as the querying user and inventory_items' RLS
-- (user_id = auth.uid()) scopes results to that user.
ALTER VIEW public.pantry_items_by_expire SET (security_invoker = true);