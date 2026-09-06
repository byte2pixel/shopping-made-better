package com.fullsail.shoppingmadebetter.feature.pantry.domain

/**
 * Why an inventory quantity changed. Mirrors the `reason` CHECK constraint on
 * `inventory_adjustments` ('auto' | 'manual' | 'confirmed' | 'undo' | 'dismissed').
 * [Auto] rows are normally written by the nightly job, not by the app.
 */
enum class AdjustmentReason {
    Auto,
    Manual,
    Confirmed,
    Undo,
    Dismissed,
    ;

    /** The raw database string for this reason, matching the `reason` CHECK constraint. */
    fun toDbValue(): String = when (this) {
        Auto -> "auto"
        Manual -> "manual"
        Confirmed -> "confirmed"
        Undo -> "undo"
        Dismissed -> "dismissed"
    }

    companion object {
        /** Maps the raw database string; `null` or unknown means no audit row. */
        fun fromDbValue(value: String?): AdjustmentReason? = when (value) {
            "auto" -> Auto
            "manual" -> Manual
            "confirmed" -> Confirmed
            "undo" -> Undo
            "dismissed" -> Dismissed
            else -> null
        }
    }
}
