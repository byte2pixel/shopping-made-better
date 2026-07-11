ALTER TABLE public.shopping_list_items ENABLE ROW LEVEL SECURITY;

GRANT INSERT ON public.shopping_list_items TO anon, authenticated;

CREATE POLICY "Shopping Items are writable by everyone"
  ON public.shopping_list_items
  FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);
