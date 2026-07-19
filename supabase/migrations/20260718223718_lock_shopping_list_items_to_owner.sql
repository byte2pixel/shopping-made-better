-- shopping_list_items previously had blanket "readable/writable by everyone"
-- policies granted to anon + authenticated (20260710181740 / 20260710183033).
-- The app now requires login and a user's list items must be private to them.
--
-- Items have no user_id of their own: ownership is indirect via the parent
-- public.shopping_lists row, which is already RLS-scoped to auth.uid()
-- (20260718025602 / 20260717232204). So the policies check that the item's
-- shopping_list_id belongs to a list owned by the caller. auth.uid() is wrapped
-- in a scalar subquery so Postgres evaluates it once (initplan) rather than
-- per row.

ALTER TABLE public.shopping_list_items ENABLE ROW LEVEL SECURITY;

-- Drop the public access and remove the anon grant.
DROP POLICY IF EXISTS "ShoppingItems are readable by everyone" ON public.shopping_list_items;
DROP POLICY IF EXISTS "Shopping Items are writable by everyone" ON public.shopping_list_items;
REVOKE SELECT, INSERT ON public.shopping_list_items FROM anon;

GRANT SELECT, INSERT ON public.shopping_list_items TO authenticated;

CREATE POLICY "Owners can read their shopping list items"
  ON public.shopping_list_items
  FOR SELECT
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.shopping_lists sl
      WHERE sl.id = shopping_list_items.shopping_list_id
        AND sl.user_id = (SELECT auth.uid())
    )
  );

CREATE POLICY "Owners can add items to their shopping lists"
  ON public.shopping_list_items
  FOR INSERT
  TO authenticated
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.shopping_lists sl
      WHERE sl.id = shopping_list_items.shopping_list_id
        AND sl.user_id = (SELECT auth.uid())
    )
  );