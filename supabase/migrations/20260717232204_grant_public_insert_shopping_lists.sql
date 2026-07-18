ALTER TABLE public.shopping_lists ENABLE ROW LEVEL SECURITY;

GRANT INSERT ON public.shopping_lists TO anon, authenticated;

CREATE POLICY "Shopping Lists are writable by everyone"
  ON public.shopping_lists
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());
