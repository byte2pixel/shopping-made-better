package com.fullsail.shoppingmadebetter.feature.pantry.domain

/**
 * Where an inventory item is stored. Mirrors the `location` CHECK constraint on
 * `inventory_items` in the database ('pantry' | 'fridge' | 'freezer').
 */
enum class PantryLocation {
    Freezer,
    Fridge,
    Pantry,
    ;

    companion object {
        /**
         * Maps the raw database string to a [PantryLocation]. Anything unrecognized
         * falls back to [Pantry].
         */
        fun fromDbValue(value: String): PantryLocation = when (value) {
            "freezer" -> Freezer
            "fridge" -> Fridge
            else -> Pantry
        }
    }

    /**
     * The raw database string for this location, matching the `location` CHECK
     * constraint on `inventory_items`. Inverse of [fromDbValue].
     */
    fun toDbValue(): String = when (this) {
        Freezer -> "freezer"
        Fridge -> "fridge"
        Pantry -> "pantry"
    }
}
