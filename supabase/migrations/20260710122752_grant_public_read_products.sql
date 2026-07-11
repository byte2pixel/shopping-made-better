
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.products TO anon, authenticated;

CREATE POLICY "Products are readable by everyone"
  ON public.products
  FOR SELECT
  TO anon, authenticated
  USING (true);