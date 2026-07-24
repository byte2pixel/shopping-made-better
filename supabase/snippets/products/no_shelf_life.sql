SELECT shelf_life_category, COUNT(*)
FROM public.products
WHERE shelf_life_days IS NULL
GROUP BY shelf_life_category;
