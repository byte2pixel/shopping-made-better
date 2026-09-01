-- ============================================================
-- Migration: estimate_consumption_rates RPC
-- ============================================================
-- Computes est_daily_rate per (user, product) from purchase history and
-- upserts into user_product_consumption. Applying rates to pantry lots
-- is SCRUM-218; nothing here mutates inventory.
--
-- Rate per pair, 'ea'-priced products only:
--   history:    (sum(qty) - avg qty per trip) / days between first and
--               last purchase, when the pair has >= 2 trips spanning
--               >= 14 days. Floored at 1/shelf_life_days.
--   shelf_life: 1 / products.shelf_life_days when the guard fails.
--   no row:     guard fails and shelf_life_days is null.
-- Rates are clamped to 1.0/day. confidence = trips/10 capped at 0.90
-- for history, fixed 0.10 for shelf_life. Rows with source = 'manual'
-- are never overwritten.
--
-- SECURITY INVOKER, no auth.uid(): user_id comes from purchase_history,
-- so RLS scopes an app caller to their own rows while the local
-- postgres role computes all users.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.estimate_consumption_rates()
RETURNS integer
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_upserted integer;
BEGIN
  WITH pair_stats AS (
    SELECT
      ph.user_id,
      phi.product_id,
      count(DISTINCT phi.purchase_id)                               AS trips,
      sum(phi.quantity)                                             AS total_qty,
      extract(day from max(ph.purchased_at) - min(ph.purchased_at)) AS span_days,
      p.shelf_life_days
    FROM public.purchase_history_items phi
    JOIN public.purchase_history ph ON ph.id = phi.purchase_id
    JOIN public.products p          ON p.id  = phi.product_id
    WHERE p.pricing_unit = 'ea'  -- skip weight-priced products
    GROUP BY ph.user_id, phi.product_id, p.shelf_life_days
  ),
  rates AS (
    SELECT
      user_id,
      product_id,
      CASE
        WHEN trips >= 2 AND span_days >= 14 THEN
          least(1.0, greatest(
            (total_qty - total_qty / trips) / span_days,
            coalesce(1.0 / shelf_life_days, 0)))
        -- least() ignores nulls, so a missing shelf life needs its own branch
        WHEN shelf_life_days IS NOT NULL THEN least(1.0, 1.0 / shelf_life_days)
        ELSE NULL
      END AS est_daily_rate,
      CASE
        WHEN trips >= 2 AND span_days >= 14 THEN 'history'
        ELSE 'shelf_life'
      END AS source,
      CASE
        WHEN trips >= 2 AND span_days >= 14 THEN least(0.90, trips::numeric / 10)
        ELSE 0.10
      END AS confidence
    FROM pair_stats
  )
  INSERT INTO public.user_product_consumption
    (user_id, product_id, est_daily_rate, source, confidence, last_computed_at)
  SELECT user_id, product_id, round(est_daily_rate, 4), source, confidence, now()
  FROM rates
  WHERE est_daily_rate IS NOT NULL
  ON CONFLICT (user_id, product_id) DO UPDATE SET
    est_daily_rate   = excluded.est_daily_rate,
    source           = excluded.source,
    confidence       = excluded.confidence,
    last_computed_at = excluded.last_computed_at
  WHERE user_product_consumption.source <> 'manual';

  GET DIAGNOSTICS v_upserted = ROW_COUNT;
  RETURN v_upserted;
END;
$$;

GRANT EXECUTE ON FUNCTION public.estimate_consumption_rates() TO authenticated;
