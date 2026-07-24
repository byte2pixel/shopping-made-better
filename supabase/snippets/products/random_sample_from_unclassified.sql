SELECT title, brand, uom
FROM public.products
WHERE shelf_life_category = 'unclassified'
ORDER BY random()
LIMIT 40;
