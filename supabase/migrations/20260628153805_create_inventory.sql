-- ============================================================
-- Migration: Pantry / Fridge Inventory
-- ============================================================

-- ------------------------------------------------------------
-- inventory_items
-- Tracks what a user currently has at home.
-- location: 'pantry' | 'fridge' | 'freezer'
-- expires_at: optional best-before / expiry date for meal
--   suggestion filtering (don't suggest using expired items).
-- ------------------------------------------------------------
CREATE TABLE public.inventory_items (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id     uuid        NOT NULL REFERENCES public.profiles(id)  ON DELETE CASCADE,
  product_id  uuid        NOT NULL REFERENCES public.products(id)  ON DELETE CASCADE,
  quantity    numeric(10, 3) NOT NULL DEFAULT 0,
  unit        text        NOT NULL,
  location    text        NOT NULL DEFAULT 'pantry'
                CHECK (location IN ('pantry', 'fridge', 'freezer')),
  purchased_at date,
  expires_at   date,
  created_at  timestamptz DEFAULT now() NOT NULL,
  updated_at  timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_inventory_user    ON public.inventory_items (user_id);
CREATE INDEX idx_inventory_expires ON public.inventory_items (expires_at) WHERE expires_at IS NOT NULL;

CREATE TRIGGER trg_inventory_updated_at
  BEFORE UPDATE ON public.inventory_items
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
