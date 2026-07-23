-- Switch the view to security_invoker so it executes as the querying
-- authenticated user; the tables RLS policy
-- (user_id = auth.uid()) then scopes the results to that user.
alter view public.shopping_trip_summaries set (security_invoker = true);
alter view public.pantry_items_by_expire set (security_invoker = true);
alter view public.create_product_pricing_comparison set (security_invoker = true);
