ALTER TABLE public.shopping_list_items ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.shopping_list_items TO anon, authenticated;

CREATE POLICY "ShoppingItems are readable by everyone"
  ON public.shopping_list_items
  FOR SELECT
  TO anon, authenticated
  USING (true);
