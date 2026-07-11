CREATE VIEW create_product_pricing_comparison AS
SELECT
    product.id AS product_id,
    product.title AS product_title,
    product.brand AS product_brand,
    storePricing.display_price AS display_price,
    storePricing.store_id AS store_id,
    store.name AS store_name
FROM products product
JOIN store_product_pricing storePricing
    ON product.id = storePricing.product_id
JOIN stores store
    ON storePricing.store_id = store.id;

grant select on public.create_product_pricing_comparison to anon, authenticated;