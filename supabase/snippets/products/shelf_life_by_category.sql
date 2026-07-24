SELECT shelf_life_category,
    COUNT(*)             AS products,
    MAX(shelf_life_days) AS days
FROM public.products
GROUP BY shelf_life_category
ORDER BY products DESC;
