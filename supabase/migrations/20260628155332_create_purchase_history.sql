-- ============================================================
-- Migration: Purchase History
-- ============================================================

-- ------------------------------------------------------------
-- purchase_history
-- Represents a completed shopping trip at a store.
-- total_amount is optional (can be derived from items).
-- ------------------------------------------------------------
CREATE TABLE public.purchase_history (
  id            uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       uuid        NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  store_id      uuid        REFERENCES public.stores(id) ON DELETE SET NULL,
  purchased_at  timestamptz NOT NULL DEFAULT now(),
  total_amount  numeric(10, 2),
  created_at    timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_purchase_history_user ON public.purchase_history (user_id, purchased_at DESC);

-- ------------------------------------------------------------
-- purchase_history_items
-- Individual line items within a purchase.
-- added_to_inventory tracks whether the item was logged to
-- the user's pantry/fridge after purchase.
-- ------------------------------------------------------------
CREATE TABLE public.purchase_history_items (
  id                  uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  purchase_id         uuid        NOT NULL REFERENCES public.purchase_history(id) ON DELETE CASCADE,
  product_id          uuid        NOT NULL REFERENCES public.products(id)         ON DELETE CASCADE,
  quantity            numeric(10, 3) NOT NULL,
  price_paid          numeric(10, 2) NOT NULL,
  added_to_inventory  boolean     NOT NULL DEFAULT false,
  created_at          timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_purchase_items_purchase ON public.purchase_history_items (purchase_id);
