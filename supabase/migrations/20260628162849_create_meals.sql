-- ============================================================
-- Migration: Meals and Ingredients
-- ============================================================

-- ------------------------------------------------------------
-- meals
-- A meal suggestion. Can be seeded manually or generated.
-- Used by the app to suggest what to cook based on current
-- inventory and what is on sale at a nearby store.
-- ------------------------------------------------------------
CREATE TABLE public.meals (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  name        text        NOT NULL,
  description text,
  image_url   text,
  created_at  timestamptz DEFAULT now() NOT NULL,
  updated_at  timestamptz DEFAULT now() NOT NULL
);

CREATE TRIGGER trg_meals_updated_at
  BEFORE UPDATE ON public.meals
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ------------------------------------------------------------
-- meal_ingredients
-- Links a meal to its required ingredients.
-- product_id is nullable to support custom/generic ingredients
-- not yet in the product catalog (e.g., "salt", "water").
-- ingredient_name is always populated for display purposes.
-- ------------------------------------------------------------
CREATE TABLE public.meal_ingredients (
  id                uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  meal_id           uuid        NOT NULL REFERENCES public.meals(id)     ON DELETE CASCADE,
  product_id        uuid        REFERENCES public.products(id)           ON DELETE SET NULL,
  ingredient_name   text        NOT NULL,
  quantity          numeric(10, 3) NOT NULL,
  unit              text        NOT NULL,
  created_at        timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_meal_ingredients_meal    ON public.meal_ingredients (meal_id);
CREATE INDEX idx_meal_ingredients_product ON public.meal_ingredients (product_id) WHERE product_id IS NOT NULL;
