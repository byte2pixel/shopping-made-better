-- shelf-stable items with a short life is suspect
SELECT title, shelf_life_category, shelf_life_days
FROM public.products
WHERE shelf_life_category IN ('canned_jarred','dry_goods','beverages',
                              'condiments_sauces_spices','baking_oils')
  AND shelf_life_days < 60
ORDER BY shelf_life_days; -- hopefully returns nothing
