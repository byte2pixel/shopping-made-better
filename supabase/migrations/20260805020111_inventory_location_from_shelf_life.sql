-- ============================================================
-- Migration: derive inventory location from product shelf life category
-- ============================================================
-- "Explicit choice wins": we only derive when location was left at the 'pantry'
-- default. An insert that names 'fridge'/'freezer' is untouched; the only lossy
-- case is explicitly choosing 'pantry' for a frozen/fridge item, which is
-- nonsensical anyway.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.location_for_shelf_life_category(p_category text)
RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE
    WHEN p_category = 'frozen' THEN 'freezer'
    WHEN p_category IN (
      'fresh_meat_seafood', 'berries', 'deli_prepared', 'fresh_herbs_greens',
      'milk_cream', 'fresh_fruit', 'fresh_vegetables', 'yogurt', 'eggs',
      'cheese', 'butter_margarine'
    ) THEN 'fridge'
    ELSE 'pantry'
  END;
$$;

-- SECURITY DEFINER so the products lookup succeeds regardless of the caller's RLS
-- context (shelf life / category are non-sensitive), same as set_inventory_expiry().
CREATE OR REPLACE FUNCTION public.set_inventory_location()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_category text;
BEGIN
  IF NEW.location = 'pantry' THEN
    SELECT shelf_life_category
      INTO v_category
      FROM public.products
     WHERE id = NEW.product_id;

    NEW.location := public.location_for_shelf_life_category(v_category);
  END IF;

  RETURN NEW;
END;
$$;

-- BEFORE INSERT ONLY (deliberately not UPDATE): the upcoming change-location feature
-- will UPDATE location, and firing on update would re-derive and fight a user who
-- sets location back to 'pantry'.
CREATE TRIGGER trg_inventory_set_location
  BEFORE INSERT ON public.inventory_items
  FOR EACH ROW EXECUTE FUNCTION public.set_inventory_location();

-- Backfill existing rows that are still at the 'pantry' default. The '= pantry'
-- guard leaves any hand-set 'fridge'/'freezer' rows alone.
UPDATE public.inventory_items ii
   SET location = public.location_for_shelf_life_category(p.shelf_life_category)
  FROM public.products p
 WHERE p.id = ii.product_id
   AND ii.location = 'pantry';