-- obviously-wrong perishables, anything fresh with a long shelf life is suspect
SELECT title, shelf_life_category, shelf_life_days
FROM public.products
WHERE shelf_life_category IN ('fresh_meat_seafood','berries','fresh_herbs_greens',
                              'milk_cream','fresh_fruit','fresh_vegetables')
  AND shelf_life_days > 30
ORDER BY shelf_life_days DESC; -- hopefully no rows returned
