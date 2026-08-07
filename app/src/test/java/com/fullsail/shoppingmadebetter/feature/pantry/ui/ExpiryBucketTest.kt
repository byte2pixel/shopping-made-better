package com.fullsail.shoppingmadebetter.feature.pantry.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Boundary tests for [expiryBucket], the pure day-count to chip severity mapping. */
class ExpiryBucketTest {

    @Test
    fun `past due or due today is expired`() {
        assertEquals(ExpiryBucket.Expired, expiryBucket(-100))
        assertEquals(ExpiryBucket.Expired, expiryBucket(-1))
        assertEquals(ExpiryBucket.Expired, expiryBucket(0))
    }

    @Test
    fun `one to two days is urgent`() {
        assertEquals(ExpiryBucket.Urgent, expiryBucket(1))
        assertEquals(ExpiryBucket.Urgent, expiryBucket(2))
    }

    @Test
    fun `three through five days is soon`() {
        assertEquals(ExpiryBucket.Soon, expiryBucket(3))
        assertEquals(ExpiryBucket.Soon, expiryBucket(4))
        assertEquals(ExpiryBucket.Soon, expiryBucket(5))
    }

    @Test
    fun `six or more days shows no chip`() {
        assertNull(expiryBucket(6))
        assertNull(expiryBucket(365))
    }

    @Test
    fun `unknown expiration shows no chip`() {
        assertNull(expiryBucket(null))
    }
}