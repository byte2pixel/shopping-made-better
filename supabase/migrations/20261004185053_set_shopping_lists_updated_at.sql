CREATE OR REPLACE FUNCTION public.set_shopping_lists_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.sort_order IS DISTINCT FROM OLD.sort_order THEN
    NEW.updated_at = OLD.updated_at;
  ELSE
    NEW.updated_at = now();
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_shopping_lists_updated_at ON public.shopping_lists;
CREATE TRIGGER trg_shopping_lists_updated_at
  BEFORE UPDATE ON public.shopping_lists
  FOR EACH ROW EXECUTE FUNCTION public.set_shopping_lists_updated_at();