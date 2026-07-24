"""
shelf_life.py
-------------
Deterministic shelf-life classifier for grocery products.

Given a product's title + description, assigns a *shelf-life category* and a
default shelf life in days. This is intentionally rule-based so that seed 
generation is fully reproducible: the same CSV in always produces the same
seed out. The keyword rules are the auditable source of truth — correct a 
mis-bucketed item by adjusting a rule, not by editing 10k rows.

Assumptions:
  * One value per product, for its *typical* storage.
    Location-aware (pantry/fridge/freezer) shelf life is a possible future
    refinement, not modelled here.
  * Days reflect roughly how long the item is good AFTER purchase. On adding an
    item to inventory, expiry is estimated as purchased_at + shelf_life_days.

Category -> days values are deliberate, realistic estimates. NULL days means
"no meaningful food expiry" (non-food) or "could not classify" — never "shelf-stable food".
Shelf-stable food gets a real, long, finite number instead of one magic sentinel.
"""

from __future__ import annotations

# ---------------------------------------------------------------------------
# Category -> default shelf life (days). None == no expiry date assigned.
# ---------------------------------------------------------------------------
CATEGORY_DAYS: dict[str, int | None] = {
    # Fresh / highly perishable
    "fresh_meat_seafood": 3,
    "berries": 5,
    "deli_prepared": 5,
    "fresh_herbs_greens": 7,
    "bakery_bread": 7,
    "milk_cream": 10,
    "fresh_fruit": 14,
    "fresh_vegetables": 14,
    "yogurt": 21,
    "eggs": 28,
    "cheese": 30,
    "root_vegetables": 30,
    "butter_margarine": 60,
    # Frozen
    "frozen": 180,
    # Shelf-stable food
    "snacks_confectionery": 120,
    "beverages": 270,
    "dry_goods": 365,
    "condiments_sauces_spices": 365,
    "baking_oils": 365,
    "canned_jarred": 730,
    # No food expiry / unknown
    "household_nonfood": None,
    "unclassified": None,
}

# ---------------------------------------------------------------------------
# Ordered rules: (category, (keyword, ...)). First matching rule wins, so the
# most specific / trap-avoiding rules come first. Keywords are lowercase
# substrings matched against a space-padded "title description" string; pad
# a keyword with spaces (e.g. " can ") to force a whole-word match.
# ---------------------------------------------------------------------------
RULES: list[tuple[str, tuple[str, ...]]] = [
    # --- Non-food first: it must never fall through into a food bucket -------
    ("household_nonfood", (
        "detergent", "laundry", "fabric softener", "dish soap", "dishwasher",
        "dishwashing", "dish liquid", "all purpose cleaner", "cleaning", "cleaner",
        "bleach", "disinfect", "shampoo", "conditioner for hair", "body wash",
        "hand soap", "bar soap", "toothpaste", "mouthwash", "deodorant",
        "antiperspirant", "body lotion", "hand lotion", "moisturizer", "sunscreen",
        "facial toner", "witch hazel", "hair colour", "hair color", "razor",
        "shaving", "paper towel", "toilet paper", "facial tissue", "aluminum foil",
        "parchment paper", "plastic wrap", "cling wrap", "food wrap", "freezer bag",
        "sandwich bag", "storage bag", "garbage bag", "trash bag", "sponge",
        "scrub pad", "diaper", "baby wipe", "wet wipe", "disinfecting wipe",
        "cat litter", "dog food", "dog treat", "cat food", "cat treat", "pet food",
        "vitamin", "supplement", "probiotic capsule", "melatonin", "homeopathic",
        "medicine", "pain relief", "antacid", "cough syrup", "cold medicine",
        "first aid", "battery", "batteries", "light bulb", "hand sanitizer",
        "cotton pad", "cotton ball", "feminine", "tampon", "panty liner",
        "bandage", "band-aid", "candle",
    )),

    # --- Frozen (beats dairy/snack/meat: ice cream, frozen pizza, etc.) ------
    ("frozen", (
        "ice cream", "gelato", "sorbet", "sherbet", "frozen", "popsicle",
        "freezie", "mochi ice", "ice bar", "ice pop", "novelty bar", "ice sandwich",
    )),

    # --- Specific overrides that would otherwise mis-bucket ------------------
    ("dry_goods", ("mac & cheese", "macaroni and cheese", "macaroni & cheese",
                   "kraft dinner")),
    ("snacks_confectionery", ("cheese cracker", "cheezies", "cheese puff",
                              "cheese curl", "cheese ring", "cheese popcorn",
                              "cheese snack", "peanut butter cup", "tortilla chip",
                              "corn chip")),
    ("canned_jarred", ("coconut milk", "coconut cream", "condensed milk",
                       "evaporated milk", "cream of mushroom", "cream of chicken",
                       "cream of celery")),
    ("beverages", ("almond milk", "soy milk", "oat milk", "cashew milk",
                   "rice milk", "coconut beverage", "cream soda")),
    ("condiments_sauces_spices", ("peanut butter", "almond butter", "nut butter",
                                  "cashew butter", "apple butter")),

    # --- Refrigerated dairy --------------------------------------------------
    ("yogurt", ("yogurt", "yoghurt", "kefir", "skyr")),
    ("milk_cream", ("2% milk", "1% milk", "whole milk", "skim milk",
                    "homogenized milk", "chocolate milk", "buttermilk",
                    "heavy cream", "whipping cream", "sour cream", "cream cheese",
                    "half and half", "half & half", "coffee cream", "creamer",
                    "table cream", "18% cream", "35% cream", "10% cream")),
    ("cheese", ("cheese", "cheddar", "mozzarella", "parmesan", "parmigiano",
                "brie", "gouda", "feta", "ricotta", "provolone", "swiss cheese",
                "havarti", "gruyere", "camembert", "mascarpone", "boursin")),
    ("eggs", ("large eggs", "dozen egg", "egg white", "free range egg",
              "free-range egg", "omega egg", "grade a egg", "liquid egg")),
    ("butter_margarine", ("salted butter", "unsalted butter", "margarine",
                          "buttery spread", "dairy spread", "plant-based butter")),

    # --- Bakery (before snacks/dry so bread/bagel don't read as dry goods) ---
    ("bakery_bread", ("bread", "bagel", "baguette", "brioche", "focaccia",
                      "ciabatta", "croissant", "english muffin", "crumpet",
                      "dinner roll", "hamburger bun", "hot dog bun", "hoagie",
                      "naan", "pita", "flatbread", "sliced loaf", "sourdough",
                      "tortilla")),

    # --- Fresh meat, seafood & deli -----------------------------------------
    ("fresh_meat_seafood", ("fresh salmon", "raw chicken", "chicken breast",
                            "chicken thigh", "ground beef", "ground turkey",
                            "ground pork", "pork chop", "pork loin", "beef steak",
                            "ribeye", "sirloin", "ground chicken", "fresh tilapia",
                            "raw shrimp", "fresh cod", "fresh trout", "fresh fish")),
    ("deli_prepared", ("deli ", "sliced ham", "chopped ham", "cooked ham",
                       "sliced turkey breast", "cold cut", "rotisserie",
                       "prepared meal", "ready to eat", "oven easy meal",
                       "fresh soup", "sushi", "hummus", "fresh salsa", "guacamole",
                       "hot dog", "wiener", "sausage", "bacon", "salami",
                       "pepperoni", "prosciutto", "charcuterie", "bratwurst")),

    # --- Shelf-stable food (processed forms BEFORE the raw produce below) ----
    ("canned_jarred", ("canned", "tinned", " can ", "in a can", "jarred", " jar ",
                       "in a jar", "broth", " stock ", "chicken stock",
                       "beef stock", "vegetable stock", "baked beans", "refried",
                       "chickpea", "garbanzo", "kidney bean", "black beans",
                       "white beans", "navy bean", "green peas", "whole green peas",
                       "hearts of palm", "diced tomato", "crushed tomato",
                       "stewed tomato", "tomato paste", "tomato sauce",
                       "pasta sauce", "pizza sauce", "salsa", "pickle", "olives",
                       "sardine", "anchov", "tuna", "canned salmon", "coconut water")),
    ("dry_goods", ("pasta", "spaghetti", "macaroni", "penne", "noodle", " rice ",
                   "quinoa", "couscous", "flour", " sugar", "cereal", "granola",
                   "oatmeal", " oats", "lentil", "split pea", "dried bean",
                   "cracker", "crispbread", "rice cake", "bread crumb",
                   "breadcrumb", "stuffing mix", "instant noodle", "ramen",
                   "dry roasted", "popping corn", "kernel")),
    ("baking_oils", ("cake mix", "muffin mix", "pancake mix", "brownie mix",
                    "baking powder", "baking soda", " yeast", "cocoa powder",
                    "vanilla extract", "olive oil", "vegetable oil", "canola oil",
                    "coconut oil", "avocado oil", "sesame oil", "cooking spray",
                    "shortening", "corn starch", "cornstarch", "icing sugar",
                    "brown sugar", "chocolate chip")),
    ("condiments_sauces_spices", ("ketchup", "mustard", "mayonnaise", "mayo",
                                 "salad dressing", "dressing", "vinaigrette",
                                 "soy sauce", "hot sauce", "bbq sauce",
                                 "barbecue sauce", "teriyaki", "marinade", "gravy",
                                 "seasoning", " spice", "curry", "masala",
                                 "chili sauce", "chili paste", "curry paste",
                                 "peri peri", "dumpling sauce", "sriracha",
                                 "relish", "syrup", " honey ", "jam", "jelly",
                                 "marmalade", "vinegar", "worcestershire", "hoisin",
                                 "fish sauce", "minced garlic", "pesto", "chutney",
                                 "sea salt", "table salt", "kosher salt",
                                 "black pepper", "peppercorn", "cayenne", "paprika",
                                 "cinnamon", "oregano", "cumin", "garlic powder",
                                 "onion powder", "garam")),
    ("beverages", ("soda", "cola", " pop ", "tonic", "ginger ale", "root beer",
                  "sparkling water", "flavoured water", "energy drink",
                  "sports drink", "juice", "iced tea", "lemonade", "kombucha",
                  "coffee", "espresso", "whole bean", "tea bag", "green tea",
                  "black tea", "herbal tea", "chai", "hot chocolate", "drink mix",
                  "water bottle", "spring water", "mineral water", "cider",
                  "beverage")),
    ("snacks_confectionery", ("chips", "popcorn", "pretzel", "cookie", "biscuit",
                             "candy", "chocolate", "gummy", "gummi", "gummies",
                             " gum ", " mint", "trail mix", "granola bar",
                             "protein bar", "fruit snack", "dried fruit", "raisin",
                             " nuts", "cashew", "almond", "peanut", "pistachio",
                             "wafer", "brittle", "toffee", "caramel", "marshmallow",
                             "snack", "jerky", "licorice", "lollipop", "smores",
                             "s'mores", "chewy bar")),

    # --- Fresh produce LAST: only raw items reach here ----------------------
    ("fresh_herbs_greens", ("cilantro", "parsley", "fresh basil", "fresh mint",
                            "fresh dill", "fresh thyme", "rosemary sprig", "chives",
                            "lettuce", "romaine", "spinach", "arugula", "kale",
                            "spring mix", "salad greens", "mixed greens",
                            "baby greens")),
    ("berries", ("strawberr", "blueberr", "raspberr", "blackberr", "cranberr",
                 "fresh berries")),
    ("root_vegetables", ("potato", "onion", "carrot", "beet", "turnip", "parsnip",
                         "rutabaga", "sweet potato", " yam", "radish", "ginger",
                         "garlic bulb", "shallot")),
    ("fresh_vegetables", ("broccoli", "cauliflower", "celery", "cucumber",
                         "zucchini", "bell pepper", "tomato", "mushroom",
                         "asparagus", "green bean", "brussels sprout", "cabbage",
                         "eggplant", "squash", "corn on the cob", "fresh herb")),
    ("fresh_fruit", ("apple", "banana", "orange", "grape", "pear", "peach",
                    "plum", "nectarine", "kiwi", "pineapple", "watermelon",
                    "cantaloupe", "honeydew", "avocado", "lemon", "lime",
                    "clementine", "mandarin", "mango", "papaya", "cherries")),
]


def _norm(text: str | None) -> str:
    """Space-padded lowercase string for whole-word substring matching."""
    return f" {(text or '').lower()} "


# Rules with the non-food bucket removed, used ONLY for the description fallback.
# Food descriptions routinely mention non-food words in passing ("enriched with
# Vitamins A and D", "great with a glass of wine") -- letting those flip a food
# to household_nonfood is a trap (e.g. 2% milk -> non-food via "vitamin"). A real
# non-food item declares itself in its TITLE, so the title pass keeps the full
# rule set; only the description pass drops the non-food bucket.
_FOOD_RULES: list[tuple[str, tuple[str, ...]]] = [
    rule for rule in RULES if rule[0] != "household_nonfood"
]


def _match(text: str, rules: list[tuple[str, tuple[str, ...]]] = RULES) -> str | None:
    """Return the first category whose keyword appears in `text`, else None."""
    for category, keywords in rules:
        for kw in keywords:
            if kw in text:
                return category
    return None


def classify(title: str | None, description: str | None = None) -> tuple[str, int | None]:
    """
    Return (category, shelf_life_days) for a product.

    The TITLE is matched first, on its own: it is the reliable signal. Product
    descriptions are recipe/marketing prose ("great with curry and salsa") that
    routinely mention *other* foods, so matching them directly mis-buckets
    thousands of items. The description is only consulted as a fallback when the
    title alone matches nothing, to rescue a few otherwise-unclassified products.

    Falls back to ('unclassified', None) when nothing matches, so uncovered
    products are queryable (WHERE shelf_life_category = 'unclassified') rather
    than silently guessed.
    """
    category = _match(_norm(title))
    if category is None and description:
        # Non-food is excluded here on purpose (see _FOOD_RULES): a food's prose
        # must not be able to demote it to household_nonfood.
        category = _match(_norm(description), _FOOD_RULES)
    if category is None:
        category = "unclassified"
    return category, CATEGORY_DAYS[category]