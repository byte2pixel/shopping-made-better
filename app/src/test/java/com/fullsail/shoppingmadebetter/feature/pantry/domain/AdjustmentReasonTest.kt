package com.fullsail.shoppingmadebetter.feature.pantry.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for [AdjustmentReason.fromDbValue]. */
class AdjustmentReasonTest {

    @Test
    fun `fromDbValue round-trips every reason`() {
        AdjustmentReason.entries.forEach { reason ->
            assertEquals(reason, AdjustmentReason.fromDbValue(reason.toDbValue()))
        }
    }

    @Test
    fun `fromDbValue returns null for null and unknown values`() {
        assertNull(AdjustmentReason.fromDbValue(null))
        assertNull(AdjustmentReason.fromDbValue("bogus"))
        assertNull(AdjustmentReason.fromDbValue("Auto"))
    }
}
