-- ============================================================
-- Migration: Households and User Profiles
-- ============================================================

-- ------------------------------------------------------------
-- households
-- Optional grouping of users (family/roommates). The head of
-- the household can manage product preferences for the group.
-- ------------------------------------------------------------
CREATE TABLE public.households (
  id          uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  name        text        NOT NULL,
  created_at  timestamptz DEFAULT now() NOT NULL,
  updated_at  timestamptz DEFAULT now() NOT NULL
);

CREATE TRIGGER trg_households_updated_at
  BEFORE UPDATE ON public.households
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ------------------------------------------------------------
-- profiles
-- Extends the Supabase auth.users table with app-specific data.
-- Automatically created on sign-up via trigger (to be added).
-- ------------------------------------------------------------
CREATE TABLE public.profiles (
  id                  uuid    PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  display_name        text    NOT NULL,
  avatar_url          text,
  preferred_store_id  uuid    REFERENCES public.stores(id)     ON DELETE SET NULL,
  household_id        uuid    REFERENCES public.households(id) ON DELETE SET NULL,
  is_household_head   boolean NOT NULL DEFAULT false,
  created_at          timestamptz DEFAULT now() NOT NULL,
  updated_at          timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_profiles_household ON public.profiles (household_id);

CREATE TRIGGER trg_profiles_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- Auto-create a profile row when a new auth user signs up
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.profiles (id, display_name)
  VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1)));
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
