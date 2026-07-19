ALTER TABLE public.shopping_lists
ALTER COLUMN user_id SET DEFAULT auth.uid();