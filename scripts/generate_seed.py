"""
generate_seed.py
----------------
Reads the January 2025 grocery CSV and outputs supabase/seed.sql.

Three mock stores are created with per-store price variations:
  - Whole Foods : base price * 1.05  (premium)
  - ALDI        : base price * 0.95  (budget)
  - Publix      : base price * 1.00  (standard)

Usage (from repo root):
    python scripts/generate_seed.py

The script expects the CSV at:
    grocery_data/grocery_data_jan_2025.csv

Output:
    supabase/seed.sql
"""

import csv
import os
import re
import sys
import uuid
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_PATH = REPO_ROOT / "supabase" / "seed.sql"


def _find_csv() -> Path:
    """
    Locate grocery_data_jan_2025.csv. We try a few candidate paths:
      1. Inside this repo root (when working directly in the main checkout)
      2. Sibling directory named 'shopping-made-better' one level up (worktrees layout)
      3. One directory above this repo root
    """
    filename = "grocery_data_jan_2025.csv"
    rel = Path("grocery_data") / filename
    candidates = [
        REPO_ROOT / rel,
        REPO_ROOT.parent / "shopping-made-better" / rel,
        REPO_ROOT.parent / rel,
    ]
    for c in candidates:
        if c.exists():
            return c
    raise FileNotFoundError(
        "Cannot find grocery_data_jan_2025.csv. Tried:\n"
        + "\n".join(f"  {c}" for c in candidates)
    )


CSV_PATH = _find_csv()

# ---------------------------------------------------------------------------
# Mock Stores
# ---------------------------------------------------------------------------
STORES = [
    {
        "id": str(uuid.uuid4()),
        "name": "Whole Foods",
        "address": "1030 N Orlando Ave",
        "city": "Winter Park",
        "state": "FL",
        "postal_code": "32789",
        "phone": "407-377-6040",
        "price_multiplier": Decimal("1.05"),
    },
    {
        "id": str(uuid.uuid4()),
        "name": "ALDI",
        "address": "6766 Aloma Ave",
        "city": "Winter Park",
        "state": "FL",
        "postal_code": "32792",
        "phone": "855-955-2534",
        "price_multiplier": Decimal("0.95"),
    },
    {
        "id": str(uuid.uuid4()),
        "name": "Publix",
        "address": "440 N Orlando Ave",
        "city": "Winter Park",
        "state": "FL",
        "postal_code": "32789",
        "phone": "407-644-1204",
        "price_multiplier": Decimal("1.00"),
    },
]

# Columns we need
COLUMNS_NEEDED = [
    "productId",
    "productImage",
    "brand",
    "title",
    "description",
    "packageSizing",
    "offerType",
    "offerId",
    "isVariant",
    "link",
    "articleNumber",
    "uom",
    "isSponsored",
    "isComplementarySponsored",
    "pricing.price",
    "pricing.displayPrice",
    "pricingUnits.type",
    "pricingUnits.unit",
    "pricingUnits.interval",
    "pricingUnits.minOrderQuantity",
]

# Columns to drop
COLUMNS_TO_DROP = {
    "pcoDeal",
    "textBadge",
    "pricing.wasPrice",
    "pricing.memberOnlyPrice",
    "pricing.mopDisplayPrice",
    "pricing.ehfTotal",
    "deal.type",
    "deal.text",
    "deal.points",
    "deal.name",
    "deal.expiryDate",
    "deal.dealPrice",
    "inventoryIndicator.indicatorId",
    "inventoryIndicator.text",
    "pricing.environmentalHandlingFee.carbonFee",
    "pricing.environmentalHandlingFee.ecologyFee",
    "pricing.environmentalHandlingFee.electronicFee",
    "pricing.environmentalHandlingFee.hazardousFee",
    "pricingUnits.maxOrderQuantity",
    "pricingUnits.weighted",
    "productBadge.badgeId",
    "productBadge.badgeClass",
    "productBadge.text",
}

BATCH_SIZE = 500  # rows per INSERT statement


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def esc(value: str) -> str:
    """Escape a string for SQL single-quote literals."""
    return value.replace("'", "''")


def sql_str(value: str | None) -> str:
    if value is None or value.strip() == "" or value.strip().lower() == "nan":
        return "NULL"
    return f"'{esc(value.strip())}'"


def sql_bool(value: str | None) -> str:
    if value is None:
        return "false"
    return "true" if value.strip().lower() in ("true", "1", "yes") else "false"


def sql_int(value: str | None, default: int = 1) -> str:
    try:
        return str(int(float(value)))
    except (TypeError, ValueError):
        return str(default)


def sql_numeric(value: str | None) -> str:
    try:
        return str(Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))
    except Exception:
        return "0.00"


def store_price(base_price: str, multiplier: Decimal) -> tuple[str, str]:
    """Return (numeric_str, display_price_str) for a store's adjusted price."""
    base = Decimal(sql_numeric(base_price))
    adjusted = (base * multiplier).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    display = f"${adjusted}"
    return str(adjusted), display


def extract_image_url(raw: str) -> str:
    """Pull the first imageUrl from the productImage list-of-dicts string."""
    match = re.search(r"imageUrl':\s*'([^']+)'", raw)
    if match:
        return match.group(1)
    # Try double-quote variant
    match = re.search(r'imageUrl":\s*"([^"]+)"', raw)
    if match:
        return match.group(1)
    return ""


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    print(f"Reading {CSV_PATH} …")
    rows: list[dict] = []

    with open(CSV_PATH, newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        for row in reader:
            rows.append(row)

    print(f"  {len(rows):,} rows loaded")

    # Deduplicate on productId
    seen: set[str] = set()
    unique_rows: list[dict] = []
    for row in rows:
        pid = row.get("productId", "").strip()
        if pid and pid not in seen:
            seen.add(pid)
            unique_rows.append(row)

    print(f"  {len(unique_rows):,} unique products after deduplication")

    # Assign a stable UUID to each product
    products: list[dict] = []
    for row in unique_rows:
        pid = row.get("productId", "").strip()
        products.append(
            {
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"grocery:{pid}")),
                "source_product_id": pid,
                "article_number": sql_int(row.get("articleNumber")),
                "title": sql_str(row.get("title")),
                "brand": sql_str(row.get("brand")),
                "description": sql_str(row.get("description")),
                "package_sizing": sql_str(row.get("packageSizing")),
                "uom": sql_str(row.get("uom")),
                "image_url": sql_str(extract_image_url(row.get("productImage", ""))),
                "source_link": sql_str(row.get("link")),
                "pricing_type": sql_str(row.get("pricingUnits.type")),
                "pricing_unit": sql_str(row.get("pricingUnits.unit")),
                "pricing_interval": sql_int(row.get("pricingUnits.interval")),
                "min_order_quantity": sql_int(row.get("pricingUnits.minOrderQuantity")),
                "is_variant": sql_bool(row.get("isVariant")),
                "base_price": row.get("pricing.price", "0"),
            }
        )

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)

    with open(OUTPUT_PATH, "w", encoding="utf-8") as out:

        out.write("-- ============================================================\n")
        out.write("-- Seed data generated from grocery_data_jan_2025.csv\n")
        out.write("-- Run: npx supabase db reset\n")
        out.write("-- ============================================================\n\n")

        # ----------------------------------------------------------------
        # Stores
        # ----------------------------------------------------------------
        out.write("-- ------------------------------------------------------------\n")
        out.write("-- Stores (3 mock grocery locations)\n")
        out.write("-- ------------------------------------------------------------\n")
        out.write("INSERT INTO public.stores (id, name, address, city, state, postal_code, phone) VALUES\n")
        store_rows = []
        for s in STORES:
            store_rows.append(
                f"  ('{s['id']}', '{esc(s['name'])}', '{esc(s['address'])}', "
                f"'{esc(s['city'])}', '{esc(s['state'])}', '{esc(s['postal_code'])}', "
                f"'{esc(s['phone'])}')"
            )
        out.write(",\n".join(store_rows))
        out.write(";\n\n")

        # ----------------------------------------------------------------
        # Products (batched)
        # ----------------------------------------------------------------
        out.write("-- ------------------------------------------------------------\n")
        out.write(f"-- Products ({len(products):,} rows from January 2025 data)\n")
        out.write("-- ------------------------------------------------------------\n")

        for batch_start in range(0, len(products), BATCH_SIZE):
            batch = products[batch_start : batch_start + BATCH_SIZE]
            out.write(
                "INSERT INTO public.products (\n"
                "  id, source_product_id, article_number, title, brand, description,\n"
                "  package_sizing, uom, image_url, source_link,\n"
                "  pricing_type, pricing_unit, pricing_interval, min_order_quantity, is_variant\n"
                ") VALUES\n"
            )
            product_vals = []
            for p in batch:
                product_vals.append(
                    f"  ('{p['uuid']}', '{esc(p['source_product_id'])}', {p['article_number']}, "
                    f"{p['title']}, {p['brand']}, {p['description']},\n"
                    f"   {p['package_sizing']}, {p['uom']}, {p['image_url']}, {p['source_link']},\n"
                    f"   {p['pricing_type']}, {p['pricing_unit']}, {p['pricing_interval']}, "
                    f"{p['min_order_quantity']}, {p['is_variant']})"
                )
            out.write(",\n".join(product_vals))
            out.write("\nON CONFLICT (source_product_id) DO NOTHING;\n\n")

        # ----------------------------------------------------------------
        # Store product pricing (batched)
        # ----------------------------------------------------------------
        out.write("-- ------------------------------------------------------------\n")
        out.write(f"-- Store product pricing ({len(products) * len(STORES):,} rows)\n")
        out.write("-- Each store has a price variation applied to the base price.\n")
        out.write("-- ------------------------------------------------------------\n")

        pricing_rows: list[str] = []

        for store in STORES:
            multiplier = store["price_multiplier"]
            for p in products:
                price_str, display = store_price(p["base_price"], multiplier)
                pricing_rows.append(
                    f"  (gen_random_uuid(), '{store['id']}', '{p['uuid']}', "
                    f"{price_str}, '{esc(display)}', CURRENT_DATE, true)"
                )

        for batch_start in range(0, len(pricing_rows), BATCH_SIZE):
            batch = pricing_rows[batch_start : batch_start + BATCH_SIZE]
            out.write(
                "INSERT INTO public.store_product_pricing\n"
                "  (id, store_id, product_id, price, display_price, effective_date, is_current)\n"
                "VALUES\n"
            )
            out.write(",\n".join(batch))
            out.write("\nON CONFLICT (store_id, product_id, effective_date) DO NOTHING;\n\n")

    print(f"\nSeed file written -> {OUTPUT_PATH}")
    print(f"  Stores:          {len(STORES)}")
    print(f"  Products:        {len(products):,}")
    print(f"  Pricing rows:    {len(pricing_rows):,}")


if __name__ == "__main__":
    main()
