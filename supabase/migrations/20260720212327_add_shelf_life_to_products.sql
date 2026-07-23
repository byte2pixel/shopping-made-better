-- ============================================================
-- Migration: add shelf life to products
-- ============================================================
-- Foundation for pantry expiry estimation. Stores an estimated
-- shelf life per product so that, when an item is added to inventory,
-- its expiry can be estimated as purchased_at + shelf_life_days.
--
-- shelf_life_days:     estimated days the product stays good AFTER purchase,
--                      for its typical storage. NULL = no meaningful food
--                      expiry (non-food) or the item could not be classified.
-- shelf_life_category: the shelf-life bucket the value came from (e.g.
--                      'yogurt', 'canned_jarred', 'household_nonfood',
--                      'unclassified'). Kept so mis-bucketed items are easy to
--                      find and correct via the classifier rules.
--
-- Values are populated by scripts/generate_seed.py using a deterministic
-- keyword classifier (scripts/shelf_life.py).
-- ------------------------------------------------------------
ALTER TABLE public.products
  ADD COLUMN shelf_life_days     int,
  ADD COLUMN shelf_life_category text;

ALTER TABLE public.products
  ADD CONSTRAINT products_shelf_life_days_positive
  CHECK (shelf_life_days IS NULL OR shelf_life_days > 0);

COMMENT ON COLUMN public.products.shelf_life_days IS
  'Estimated days good after purchase (typical storage). NULL = non-food / no expiry / unclassified.';
COMMENT ON COLUMN public.products.shelf_life_category IS
  'Shelf-life bucket the estimate came from (see scripts/shelf_life.py).';