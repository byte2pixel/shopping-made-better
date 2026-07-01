-- ============================================================
-- Migration: User Product Preferences
-- ============================================================

CREATE TYPE public.product_preference_type AS ENUM (
  'favorite',     -- show first in search results
  'do_not_show',  -- hide from search results
  'do_not_buy'    -- visible but flagged; blocked from auto-add
);

CREATE TYPE public.preference_scope AS ENUM (
  'personal',   -- applies to this user only
  'household'   -- applies to all household members (head only)
);

-- ------------------------------------------------------------
-- user_product_preferences
-- A user's per-product preferences. Household heads can set
-- household-scoped rules that apply to all members.
-- ------------------------------------------------------------
CREATE TABLE public.user_product_preferences (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id     uuid        NOT NULL REFERENCES public.profiles(id)  ON DELETE CASCADE,
  product_id  uuid        NOT NULL REFERENCES public.products(id)  ON DELETE CASCADE,
  preference  public.product_preference_type NOT NULL,
  scope       public.preference_scope        NOT NULL DEFAULT 'personal',
  created_at  timestamptz DEFAULT now() NOT NULL,
  updated_at  timestamptz DEFAULT now() NOT NULL,
  UNIQUE (user_id, product_id, scope)
);

CREATE INDEX idx_prefs_user    ON public.user_product_preferences (user_id);
CREATE INDEX idx_prefs_product ON public.user_product_preferences (product_id);

CREATE TRIGGER trg_prefs_updated_at
  BEFORE UPDATE ON public.user_product_preferences
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
