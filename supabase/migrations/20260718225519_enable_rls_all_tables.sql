-- ============================================================
-- Migration: Enable RLS on every remaining public table
-- ============================================================
-- Now that login/sign-up work, no table in the public schema should be left
-- without row level security. stores, products, store_product_pricing,
-- shopping_lists and shopping_list_items were handled by earlier migrations.
-- This migration covers the rest.
--
-- Per-user tables get an owner "FOR ALL" policy (user_id = auth.uid()).
-- Child tables inherit ownership from their parent via EXISTS. Reference
-- tables (meals) are readable by any authenticated user. Tables for features
-- not yet built (households, shopping_list_shares) get RLS enabled with no
-- policy -- i.e. deny-all -- and no anon/authenticated grant, so they stay
-- locked until those features ship. auth.uid() is wrapped in a scalar
-- subquery so Postgres evaluates it once per statement.

-- ------------------------------------------------------------
-- profiles: a user can read and update only their own profile.
-- INSERT is handled by the SECURITY DEFINER handle_new_user() trigger,
-- so no insert policy is needed for the authenticated role.
-- ------------------------------------------------------------
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
GRANT SELECT, UPDATE ON public.profiles TO authenticated;

CREATE POLICY "Users can read their own profile"
  ON public.profiles
  FOR SELECT
  TO authenticated
  USING (id = (SELECT auth.uid()));

CREATE POLICY "Users can update their own profile"
  ON public.profiles
  FOR UPDATE
  TO authenticated
  USING (id = (SELECT auth.uid()))
  WITH CHECK (id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- inventory_items: owner-only. Required for pantry_items_by_expire
-- once that view runs as security_invoker.
-- ------------------------------------------------------------
ALTER TABLE public.inventory_items ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.inventory_items TO authenticated;

CREATE POLICY "Users manage their own inventory items"
  ON public.inventory_items
  FOR ALL
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- purchase_history: owner-only.
-- ------------------------------------------------------------
ALTER TABLE public.purchase_history ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.purchase_history TO authenticated;

CREATE POLICY "Users manage their own purchase history"
  ON public.purchase_history
  FOR ALL
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- purchase_history_items: ownership via the parent purchase_history row.
-- ------------------------------------------------------------
ALTER TABLE public.purchase_history_items ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.purchase_history_items TO authenticated;

CREATE POLICY "Users manage items in their own purchases"
  ON public.purchase_history_items
  FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.purchase_history ph
      WHERE ph.id = purchase_history_items.purchase_id
        AND ph.user_id = (SELECT auth.uid())
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.purchase_history ph
      WHERE ph.id = purchase_history_items.purchase_id
        AND ph.user_id = (SELECT auth.uid())
    )
  );

-- ------------------------------------------------------------
-- user_product_preferences: owner-only. (Household-scoped preferences
-- will need an additional policy once the household feature ships.)
-- ------------------------------------------------------------
ALTER TABLE public.user_product_preferences ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_product_preferences TO authenticated;

CREATE POLICY "Users manage their own product preferences"
  ON public.user_product_preferences
  FOR ALL
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- shopping_lists: select + insert policies already exist
-- (20260718025602 / 20260717232204). Round out CRUD so owners can
-- rename, complete (completed_at) and delete their own lists.
-- ------------------------------------------------------------
GRANT UPDATE, DELETE ON public.shopping_lists TO authenticated;

CREATE POLICY "Individuals can update their own shopping lists"
  ON public.shopping_lists
  FOR UPDATE
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

CREATE POLICY "Individuals can delete their own shopping lists"
  ON public.shopping_lists
  FOR DELETE
  TO authenticated
  USING (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- meals + meal_ingredients: shared reference data, readable by any
-- authenticated user (same model as stores/products).
-- ------------------------------------------------------------
ALTER TABLE public.meals ENABLE ROW LEVEL SECURITY;
GRANT SELECT ON public.meals TO authenticated;

CREATE POLICY "Meals are readable by authenticated users"
  ON public.meals
  FOR SELECT
  TO authenticated
  USING (true);

ALTER TABLE public.meal_ingredients ENABLE ROW LEVEL SECURITY;
GRANT SELECT ON public.meal_ingredients TO authenticated;

CREATE POLICY "Meal ingredients are readable by authenticated users"
  ON public.meal_ingredients
  FOR SELECT
  TO authenticated
  USING (true);

-- ------------------------------------------------------------
-- households + shopping_list_shares: features not yet built. Enable RLS
-- (deny-all, no grant) so nothing is exposed. we will need to
-- add owner/member policies when the household and list-sharing
-- features are implemented.
-- ------------------------------------------------------------
ALTER TABLE public.households ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shopping_list_shares ENABLE ROW LEVEL SECURITY;