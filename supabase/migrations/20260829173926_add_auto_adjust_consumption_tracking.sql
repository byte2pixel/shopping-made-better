-- ============================================================
-- Migration: storage for automatic pantry quantity adjustment
-- ============================================================
-- Purchase history records acquisition only -- nothing in the schema records
-- consumption -- so any automatic decrement is an ESTIMATE. This migration adds
-- the storage that estimate needs; no estimator, RPC or UI ships here.
--
--   user_product_consumption : the estimated daily consumption rate, per
--     (user, product). Mirrors user_product_stock_settings
--     (20260815175832) -- the existing precedent for a per-(user, product)
--     setting that must survive a pantry lot being removed and re-added.
--   inventory_adjustments    : audit trail. Adjustments apply silently, so
--     these rows are the only record of why a quantity changed, and the
--     source for undo and the weekly digest.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- user_product_consumption
-- source: 'history'    -> derived from repeat-purchase intervals
--         'shelf_life' -> fallback, 1 / products.shelf_life_days
--         'manual'     -> corrected by the user
-- ------------------------------------------------------------
CREATE TABLE public.user_product_consumption (
  id               uuid          DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id          uuid          NOT NULL DEFAULT auth.uid()
                     REFERENCES public.profiles(id) ON DELETE CASCADE,
  product_id       uuid          NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  est_daily_rate   numeric(10,4) NOT NULL CHECK (est_daily_rate >= 0),
  source           text          NOT NULL
                     CHECK (source IN ('history', 'shelf_life', 'manual')),
  confidence       numeric(3,2)  CHECK (confidence >= 0 AND confidence <= 1),
  last_computed_at timestamptz,
  created_at       timestamptz   DEFAULT now() NOT NULL,
  updated_at       timestamptz   DEFAULT now() NOT NULL,
  -- Load-bearing: the estimator upserts on this pair.
  UNIQUE (user_id, product_id)
);

CREATE INDEX idx_consumption_user ON public.user_product_consumption (user_id);

CREATE TRIGGER trg_consumption_updated_at
  BEFORE UPDATE ON public.user_product_consumption
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- RLS
ALTER TABLE public.user_product_consumption ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_product_consumption TO authenticated;

CREATE POLICY "Users manage their own consumption rates"
  ON public.user_product_consumption
  FOR ALL
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- inventory_adjustments
-- delta is signed: negative for consumption, positive for a restore/undo.
-- reason: 'auto'      -> applied silently by the estimator
--         'manual'    -> the user edited the quantity directly
--         'confirmed' -> the user corrected an estimate
--         'undo'      -> compensating row reversing an earlier adjustment
--         'dismissed' -> the user rejected an estimate
-- ------------------------------------------------------------
CREATE TABLE public.inventory_adjustments (
  id                uuid          DEFAULT gen_random_uuid() PRIMARY KEY,
  inventory_item_id uuid          NOT NULL
                      REFERENCES public.inventory_items(id) ON DELETE CASCADE,
  user_id           uuid          NOT NULL DEFAULT auth.uid()
                      REFERENCES public.profiles(id) ON DELETE CASCADE,
  delta             numeric(10,3) NOT NULL,
  reason            text          NOT NULL
                      CHECK (reason IN ('auto', 'manual', 'confirmed', 'undo', 'dismissed')),
  created_at        timestamptz   DEFAULT now() NOT NULL
);

CREATE INDEX idx_adjustments_user ON public.inventory_adjustments (user_id);

-- Undo reads the most recent adjustment for one lot; the weekly digest reads a
-- date range of them. Both are this index.
CREATE INDEX idx_adjustments_item ON public.inventory_adjustments (inventory_item_id, created_at DESC);

-- RLS
ALTER TABLE public.inventory_adjustments ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.inventory_adjustments TO authenticated;

CREATE POLICY "Users manage their own inventory adjustments"
  ON public.inventory_adjustments
  FOR ALL
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- inventory_items: lazy catch-up bookkeeping.
--
-- last_auto_adjusted_at: when the estimator last ran against this lot. The
--   elapsed time since it is what the next run multiplies by the rate, so
--   stamping it is what stops a re-run double-applying. NULL until first
--   adjusted; callers fall back to purchased_at.
--
-- pending_fraction: adjustments accrue FRACTIONALLY instead of rounding to
--   whole units per run. Rounding per run is what would make long-shelf-life
--   goods inert -- at 1/365 per day a dry good never reaches a whole unit in
--   one step. The remainder accumulates here and converts to a whole unit when
--   it crosses 1.0. The maths stays fractional; the display stays rounded.
--   Guarded at >= 0 only: it is logically always in [0,1), but undo has to
--   unwind an accrued fraction and a hard upper bound risks blocking that.
-- ------------------------------------------------------------
ALTER TABLE public.inventory_items
  ADD COLUMN last_auto_adjusted_at timestamptz,
  ADD COLUMN pending_fraction      numeric(10,4) NOT NULL DEFAULT 0
                                     CHECK (pending_fraction >= 0);

-- ------------------------------------------------------------
-- profiles: the opt-out. Lives on the row rather than in device-local
-- settings because the adjustment RPC reads it server-side to decide whether
-- to do anything at all. Defaults to false -- the feature is opt-in until the
-- estimator is validated against real corrections.
-- ------------------------------------------------------------
ALTER TABLE public.profiles
  ADD COLUMN auto_adjust_enabled boolean NOT NULL DEFAULT false;

-- ------------------------------------------------------------
-- Recreate the pantry view to surface the estimate's provenance, so the UI can
-- mark an auto-adjusted quantity as an estimate rather than a fact the user
-- entered, and explain what it was based on. Adding columns needs DROP+CREATE;
-- CREATE OR REPLACE VIEW rejects a changed column list (42P16).
--
-- pending_fraction is deliberately NOT exposed: it is server-side maths the UI
-- never displays. last_auto_adjusted_at is exposed as epoch seconds, matching
-- "purchasedAtEpoch" in 20260826021500_create_purchase_history_summary_view.
-- ------------------------------------------------------------
DROP VIEW IF EXISTS public.pantry_items_by_expire;

CREATE VIEW public.pantry_items_by_expire AS
SELECT
    ii.id                       AS id,
    ii.product_id               AS "productId",
    p.title                     AS name,
    COALESCE(p.brand, '')       AS brand,
    COALESCE(p.description, '') AS description,
    p.package_sizing            AS size,
    ii.quantity::int            AS quantity,
    COALESCE(p.image_url, '')   AS "imageUrl",
    ii.expires_at               AS "expiryDate",
    ii.location                 AS location,
    s.low_stock_threshold       AS "lowStockThreshold",
    EXTRACT(EPOCH FROM ii.last_auto_adjusted_at)::bigint
                                AS "lastAutoAdjustedAtEpoch",
    c.source                    AS "estimateSource"
FROM public.inventory_items ii
JOIN public.products p ON p.id = ii.product_id
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = ii.product_id AND s.user_id = ii.user_id
LEFT JOIN public.user_product_consumption c
    ON c.product_id = ii.product_id AND c.user_id = ii.user_id
ORDER BY ii.expires_at ASC NULLS LAST;

GRANT SELECT ON public.pantry_items_by_expire TO authenticated;

ALTER VIEW public.pantry_items_by_expire SET (security_invoker = true);
