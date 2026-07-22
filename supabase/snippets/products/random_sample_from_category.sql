SELECT title, brand, shelf_life_days
FROM public.products
WHERE shelf_life_category = 'canned_jarred'   -- try 'cheese', 'fresh_fruit', 'beverages', …
ORDER BY random()
LIMIT 30;
