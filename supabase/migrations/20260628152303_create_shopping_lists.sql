-- ============================================================
-- Migration: Shopping Lists
-- ============================================================

-- ------------------------------------------------------------
-- shopping_lists
-- A user's named shopping list, optionally tied to a store.
-- completed_at is set when the shopping trip is finished.
-- ------------------------------------------------------------
CREATE TABLE public.shopping_lists (
  id            uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id       uuid        NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  store_id      uuid        REFERENCES public.stores(id) ON DELETE SET NULL,
  name          text        NOT NULL,
  is_shared     boolean     NOT NULL DEFAULT false,
  completed_at  timestamptz,
  created_at    timestamptz DEFAULT now() NOT NULL,
  updated_at    timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_shopping_lists_user ON public.shopping_lists (user_id);

CREATE TRIGGER trg_shopping_lists_updated_at
  BEFORE UPDATE ON public.shopping_lists
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ------------------------------------------------------------
-- shopping_list_items
-- Individual products on a list. While at the store the user
-- checks items off (is_checked). add_to_inventory controls
-- whether purchase flows into the pantry/fridge inventory.
-- ------------------------------------------------------------
CREATE TABLE public.shopping_list_items (
  id                  uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  shopping_list_id    uuid        NOT NULL REFERENCES public.shopping_lists(id) ON DELETE CASCADE,
  product_id          uuid        NOT NULL REFERENCES public.products(id)       ON DELETE CASCADE,
  quantity            numeric(10, 3) NOT NULL DEFAULT 1,
  note                text,
  is_checked          boolean     NOT NULL DEFAULT false,
  add_to_inventory    boolean     NOT NULL DEFAULT true,
  created_at          timestamptz DEFAULT now() NOT NULL,
  updated_at          timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_list_items_list ON public.shopping_list_items (shopping_list_id);

CREATE TRIGGER trg_list_items_updated_at
  BEFORE UPDATE ON public.shopping_list_items
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ------------------------------------------------------------
-- shopping_list_shares
-- Allows a list owner to share their list with other users,
-- optionally granting edit permissions.
-- ------------------------------------------------------------
CREATE TABLE public.shopping_list_shares (
  id                    uuid    DEFAULT gen_random_uuid() PRIMARY KEY,
  shopping_list_id      uuid    NOT NULL REFERENCES public.shopping_lists(id) ON DELETE CASCADE,
  shared_with_user_id   uuid    NOT NULL REFERENCES public.profiles(id)       ON DELETE CASCADE,
  can_edit              boolean NOT NULL DEFAULT false,
  created_at            timestamptz DEFAULT now() NOT NULL,
  UNIQUE (shopping_list_id, shared_with_user_id)
);
