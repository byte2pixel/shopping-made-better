-- The app now requires login, so the anon (pre-auth) API role is no longer used.
-- Remove anon SELECT from the public reference data and recreate the read
-- policies for the authenticated role only. Store/product/pricing data stays
-- readable by every logged-in user; it is shared reference data, not per-user.

-- stores -----------------------------------------------------------------
REVOKE SELECT ON public.stores FROM anon;
DROP POLICY IF EXISTS "Stores are readable by everyone" ON public.stores;
CREATE POLICY "Stores are readable by authenticated users"
  ON public.stores
  FOR SELECT
  TO authenticated
  USING (true);

-- products ---------------------------------------------------------------
REVOKE SELECT ON public.products FROM anon;
DROP POLICY IF EXISTS "Products are readable by everyone" ON public.products;
CREATE POLICY "Products are readable by authenticated users"
  ON public.products
  FOR SELECT
  TO authenticated
  USING (true);

-- views: privileges only (views carry no RLS policies of their own) -------
REVOKE SELECT ON public.shopping_trip_summaries FROM anon;
REVOKE SELECT ON public.create_product_pricing_comparison FROM anon;
REVOKE SELECT ON public.pantry_items_by_expire FROM anon;