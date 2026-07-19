ALTER TABLE public.shopping_lists ENABLE ROW LEVEL SECURITY;

GRANT INSERT ON public.shopping_lists TO authenticated;

CREATE POLICY "Individuals can only insert to their own shopping list"
  ON public.shopping_lists
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());
