-- ============================================================
-- SCRUM-286: household branch on shopping list policies and view
-- ============================================================
-- Every list is visible to the whole household. Members add, check,
-- change and delete items on any household list; the list row keeps its
-- creator, who alone renames or deletes it, and lists are inserted only
-- as yourself. Items take the household branch on all four commands
-- because complete_shopping_trip deletes checked rows under RLS and
-- would otherwise leave them behind when a housemate completes the list;
-- that housemate owns the resulting trip and lots. is_shared and
-- shopping_list_shares stay unused.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- shopping_lists. Only SELECT widens; INSERT, UPDATE and DELETE stay
-- owner-only.
-- ------------------------------------------------------------
DROP POLICY "Individuals can only view their own shopping lists" ON public.shopping_lists;

CREATE POLICY "Household members read lists"
  ON public.shopping_lists
  FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT public.household_member_ids()));

-- ------------------------------------------------------------
-- shopping_list_items. The member check sits inside the EXISTS on the
-- parent so item writes do not depend on the parent's SELECT policy.
-- ------------------------------------------------------------
DROP POLICY "Owners can read their shopping list items"                 ON public.shopping_list_items;
DROP POLICY "Owners can add items to their shopping lists"              ON public.shopping_list_items;
DROP POLICY "User can update their shopping list items"                 ON public.shopping_list_items;
DROP POLICY "Individuals can delete view their own shopping list items" ON public.shopping_list_items;

CREATE POLICY "Household members read list items"
  ON public.shopping_list_items
  FOR SELECT
  TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.shopping_lists sl
    WHERE sl.id = shopping_list_items.shopping_list_id
      AND sl.user_id IN (SELECT public.household_member_ids())));

CREATE POLICY "Household members add list items"
  ON public.shopping_list_items
  FOR INSERT
  TO authenticated
  WITH CHECK (EXISTS (
    SELECT 1 FROM public.shopping_lists sl
    WHERE sl.id = shopping_list_items.shopping_list_id
      AND sl.user_id IN (SELECT public.household_member_ids())));

CREATE POLICY "Household members update list items"
  ON public.shopping_list_items
  FOR UPDATE
  TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.shopping_lists sl
    WHERE sl.id = shopping_list_items.shopping_list_id
      AND sl.user_id IN (SELECT public.household_member_ids())))
  WITH CHECK (EXISTS (
    SELECT 1 FROM public.shopping_lists sl
    WHERE sl.id = shopping_list_items.shopping_list_id
      AND sl.user_id IN (SELECT public.household_member_ids())));

CREATE POLICY "Household members delete list items"
  ON public.shopping_list_items
  FOR DELETE
  TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.shopping_lists sl
    WHERE sl.id = shopping_list_items.shopping_list_id
      AND sl.user_id IN (SELECT public.household_member_ids())));

-- ------------------------------------------------------------
-- shopping_trip_summaries. created_by and is_own are appended; the
-- household_members join runs as owner, so a housemate's name is visible
-- here without widening profiles. DROP+CREATE (42P16).
-- ------------------------------------------------------------
DROP VIEW IF EXISTS public.shopping_trip_summaries;

CREATE VIEW public.shopping_trip_summaries AS
SELECT
  sl.id           AS shopping_list_id,
  sl.name         AS list_name,
  sl.sort_order   AS sort_order,
  sl.created_at   AS created_at,
  sl.updated_at   AS updated_at,
  s.id            AS store_id,
  s.name          AS store_name,
  count(sli.id)   AS item_count,
  coalesce(sum(spp.price * sli.quantity), 0) AS total_cost,
  hm.display_name AS created_by,
  coalesce(sl.user_id = (SELECT auth.uid()), false) AS is_own
FROM public.shopping_lists sl
JOIN public.stores s ON s.id = sl.store_id
LEFT JOIN public.household_members hm ON hm.id = sl.user_id
LEFT JOIN public.shopping_list_items sli ON sli.shopping_list_id = sl.id
LEFT JOIN public.store_product_pricing spp
       ON spp.product_id = sli.product_id
      AND spp.store_id   = sl.store_id
      AND spp.is_current = true
GROUP BY sl.id, sl.name, sl.sort_order, sl.created_at, sl.updated_at, sl.user_id,
         s.id, s.name, hm.display_name;

GRANT SELECT ON public.shopping_trip_summaries TO authenticated;
REVOKE SELECT ON public.shopping_trip_summaries FROM anon;
ALTER VIEW public.shopping_trip_summaries SET (security_invoker = true);

-- ------------------------------------------------------------
-- shopping_list_item_named_view. Unchanged columns; the option is
-- restated so reloptions reads security_invoker=true like every other
-- view.
-- ------------------------------------------------------------
ALTER VIEW public.shopping_list_item_named_view SET (security_invoker = true);
