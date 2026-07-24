-- simulated expiry if purchased today
SELECT title,
       shelf_life_category,
       shelf_life_days,
       (CURRENT_DATE + shelf_life_days) AS est_expires_at
FROM public.products
WHERE shelf_life_days IS NOT NULL
ORDER BY shelf_life_days
LIMIT 25; -- shortest-lived items expire soonest
