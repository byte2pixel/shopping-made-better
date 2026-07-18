ALTER TABLE public.shopping_lists ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.shopping_lists TO anon, authenticated;

CREATE POLICY "shoppinglists are readable by everyone"
  ON public.shopping_lists
  FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());
