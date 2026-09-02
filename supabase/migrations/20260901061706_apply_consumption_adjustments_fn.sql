-- ============================================================
-- Migration: apply_consumption_adjustments function
-- ============================================================
-- Converts elapsed time into pantry decrements using the rates from
-- estimate_consumption_rates (20260901024946). Not an app-callable RPC:
-- EXECUTE is revoked below and the nightly pg_cron job (next migration,
-- runs as postgres) is the sole caller.
--
-- Per (user, product) with a rate, an opted-in owner and an 'ea'-priced
-- product:
--   budget    = sum(pending_fraction over lots) + est_daily_rate * elapsed
--   elapsed   = fractional days since max(last_auto_adjusted_at) over the
--               lots, else min(purchased_at) over in-stock lots. Uncapped.
--   removal   = floor(budget) whole units, FEFO (expires_at ASC NULLS
--               LAST, id), capped per lot at its quantity, floored at 0.
--               Rows are never deleted.
--   remainder = budget - removed, stored on the FEFO-first lot still in
--               stock, other pendings zeroed; discarded when no lot
--               survives.
--   stamp     = last_auto_adjusted_at = now() on every processed lot, so
--               an immediate rerun removes nothing.
--   audit     = inventory_adjustments (reason 'auto', negative delta) per
--               lot that lost whole units.
-- Untouched: weight-priced products, pairs without a rate, opted-out
-- users, zero-quantity zero-pending lots.
--
-- The UPDATE also fires trg_inventory_set_expiry (backfills a NULL
-- expires_at) and trg_inventory_updated_at.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.apply_consumption_adjustments()
RETURNS TABLE (
  inventory_item_id uuid,
  product_id        uuid,
  user_id           uuid,
  delta             numeric,
  new_quantity      numeric
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  -- Column references stay table-qualified: the RETURNS TABLE names
  -- shadow as plpgsql variables.
  RETURN QUERY
  WITH lots AS (
    SELECT ii.id, ii.user_id, ii.product_id, ii.quantity, ii.pending_fraction,
           ii.expires_at, ii.purchased_at, ii.last_auto_adjusted_at,
           c.est_daily_rate
    FROM public.inventory_items ii
    JOIN public.user_product_consumption c
      ON c.user_id = ii.user_id AND c.product_id = ii.product_id
    JOIN public.profiles pr ON pr.id = ii.user_id AND pr.auto_adjust_enabled
    JOIN public.products p  ON p.id = ii.product_id AND p.pricing_unit = 'ea'
    WHERE ii.quantity > 0 OR ii.pending_fraction > 0
    FOR UPDATE OF ii
  ),
  grp AS (
    SELECT l.user_id, l.product_id,
           sum(l.pending_fraction) AS pending_sum,
           min(l.est_daily_rate)   AS rate,  -- constant per pair
           coalesce(
             max(l.last_auto_adjusted_at),
             (min(l.purchased_at) FILTER (WHERE l.quantity > 0))::timestamptz
           ) AS anchor
    FROM lots l
    GROUP BY l.user_id, l.product_id
  ),
  budgeted AS (
    -- greatest() ignores a NULL anchor: no accrual, the stamp starts the clock
    SELECT g.user_id, g.product_id,
           g.pending_sum + g.rate * greatest(
             0, extract(epoch FROM (now() - g.anchor)) / 86400.0
           ) AS budget
    FROM grp g
  ),
  alloc AS (
    -- each lot takes what floor(budget) leaves after FEFO predecessors
    SELECT l.id, l.user_id, l.product_id, l.quantity, b.budget,
           least(l.quantity,
                 greatest(0, floor(b.budget)
                             - coalesce(sum(l.quantity) OVER w_prev, 0))
           ) AS removed,
           row_number() OVER w AS fefo_rank
    FROM lots l
    JOIN budgeted b
      ON b.user_id = l.user_id AND b.product_id = l.product_id
    WINDOW
      w      AS (PARTITION BY l.user_id, l.product_id
                 ORDER BY l.expires_at ASC NULLS LAST, l.id),
      w_prev AS (w ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)
  ),
  final AS (
    -- remainder lands on the FEFO-first lot still in stock, others zero
    SELECT a.id, a.user_id, a.product_id, a.removed,
           a.quantity - a.removed AS new_qty,
           CASE
             WHEN a.quantity - a.removed > 0
              AND row_number() OVER (
                    PARTITION BY a.user_id, a.product_id,
                                 (a.quantity - a.removed > 0)
                    ORDER BY a.fefo_rank) = 1
             THEN a.budget - floor(a.budget)
             ELSE 0
           END AS new_pending
    FROM alloc a
  ),
  upd AS (
    UPDATE public.inventory_items ii
       SET quantity              = f.new_qty,
           pending_fraction      = f.new_pending,
           last_auto_adjusted_at = now()
      FROM final f
     WHERE ii.id = f.id
  ),
  audit AS (
    INSERT INTO public.inventory_adjustments
      (inventory_item_id, user_id, delta, reason)
    SELECT f.id, f.user_id, -f.removed, 'auto'
    FROM final f
    WHERE f.removed > 0
  )
  SELECT f.id, f.product_id, f.user_id, -f.removed, f.new_qty
  FROM final f
  WHERE f.removed > 0
  ORDER BY f.user_id, f.product_id, f.id;
END;
$$;

-- Functions default EXECUTE to PUBLIC; only the cron job may run this.
REVOKE EXECUTE ON FUNCTION public.apply_consumption_adjustments()
  FROM PUBLIC, anon, authenticated;
