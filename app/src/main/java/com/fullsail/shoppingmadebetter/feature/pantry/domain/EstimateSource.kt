package com.fullsail.shoppingmadebetter.feature.pantry.domain

/**
 * What a lot's consumption estimate was based on. Mirrors the `source` CHECK
 * constraint on `user_product_consumption` ('history' | 'shelf_life' | 'manual').
 */
enum class EstimateSource {
    History,
    ShelfLife,
    Manual,
    ;

    companion object {
        /**
         * Maps the raw database string to an [EstimateSource]; `null` or anything
         * unrecognized means the lot has no consumption estimate.
         */
        fun fromDbValue(value: String?): EstimateSource? = when (value) {
            "history" -> History
            "shelf_life" -> ShelfLife
            "manual" -> Manual
            else -> null
        }
    }
}
