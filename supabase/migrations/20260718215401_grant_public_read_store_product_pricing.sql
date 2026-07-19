ALTER TABLE public.store_product_pricing ENABLE ROW LEVEL SECURITY;

GRANT SELECT ON public.store_product_pricing TO authenticated;

CREATE POLICY "product pricing are readable by authenticated users"
  ON public.store_product_pricing
  FOR SELECT
  TO authenticated
  USING (true);
