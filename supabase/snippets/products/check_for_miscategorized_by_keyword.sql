-- mis-buckets by keyword,  does the category match what the title implies?
SELECT title, shelf_life_category, shelf_life_days
FROM public.products
WHERE title ILIKE '%juice%' -- expect beverages; swap: '%chips%', '%sauce%', '%frozen%', '%cheese%'
ORDER BY shelf_life_category;
