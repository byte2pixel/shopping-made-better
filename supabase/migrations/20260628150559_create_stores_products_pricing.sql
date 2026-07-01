-- ============================================================
-- Migration: Stores, Products, and Store Product Pricing
-- ============================================================

-- ------------------------------------------------------------
-- stores
-- Represents the 3 mock grocery store locations used in the app.
-- ------------------------------------------------------------
CREATE TABLE public.stores (
  id              uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  name            text        NOT NULL,
  address         text        NOT NULL,
  city            text        NOT NULL,
  state           text        NOT NULL,
  postal_code     text        NOT NULL,
  phone           text,
  created_at      timestamptz DEFAULT now() NOT NULL,
  updated_at      timestamptz DEFAULT now() NOT NULL
);

-- ------------------------------------------------------------
-- products
-- Core product catalog sourced from grocery CSV data.
-- source_product_id: original productId (e.g. "20091825001_EA")
-- pricing_type: SOLD_BY_EACH | SOLD_BY_EACH_PRICED_BY_WEIGHT | SOLD_BY_WEIGHT
-- uom: unit of measure code (EA, KG, C12, C24 …)
-- pricing_unit: unit used in pricing display (ea, kg, g)
-- ------------------------------------------------------------
CREATE TABLE public.products (
  id                    uuid    DEFAULT gen_random_uuid() PRIMARY KEY,
  source_product_id     text    NOT NULL UNIQUE,
  article_number        bigint  NOT NULL,
  title                 text    NOT NULL,
  brand                 text,
  description           text,
  package_sizing        text    NOT NULL,
  uom                   text    NOT NULL,
  image_url             text,
  source_link           text    NOT NULL,
  pricing_type          text    NOT NULL,
  pricing_unit          text    NOT NULL,
  pricing_interval      int     NOT NULL DEFAULT 1,
  min_order_quantity    int     NOT NULL DEFAULT 1,
  is_variant            boolean NOT NULL DEFAULT false,
  created_at            timestamptz DEFAULT now() NOT NULL,
  updated_at            timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_products_article_number ON public.products (article_number);
CREATE INDEX idx_products_brand          ON public.products (brand);
CREATE INDEX idx_products_title          ON public.products USING gin (to_tsvector('english', title));

-- ------------------------------------------------------------
-- store_product_pricing
-- Per-store pricing for each product. Supports price history
-- via effective_date; only rows with is_current = true are live.
-- A unique constraint prevents duplicate pricing entries per
-- store/product/date combination.
-- ------------------------------------------------------------
CREATE TABLE public.store_product_pricing (
  id              uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  store_id        uuid        NOT NULL REFERENCES public.stores(id)   ON DELETE CASCADE,
  product_id      uuid        NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  price           numeric(10, 2) NOT NULL,
  display_price   text        NOT NULL,
  effective_date  date        NOT NULL DEFAULT CURRENT_DATE,
  is_current      boolean     NOT NULL DEFAULT true,
  created_at      timestamptz DEFAULT now() NOT NULL,
  UNIQUE (store_id, product_id, effective_date)
);

CREATE INDEX idx_store_pricing_store_product ON public.store_product_pricing (store_id, product_id) WHERE is_current = true;

-- Auto-update updated_at helper
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_stores_updated_at
  BEFORE UPDATE ON public.stores
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_products_updated_at
  BEFORE UPDATE ON public.products
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
