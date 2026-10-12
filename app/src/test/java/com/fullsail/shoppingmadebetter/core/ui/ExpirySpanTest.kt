package com.fullsail.shoppingmadebetter.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [expirySpan] at each unit boundary. */
class ExpirySpanTest {

    @Test
    fun pastAndPresentAreTheirOwnSpans() {
        assertEquals(ExpirySpan.Expired, expirySpan(-1))
        assertEquals(ExpirySpan.Expired, expirySpan(-400))
        assertEquals(ExpirySpan.Today, expirySpan(0))
    }

    @Test
    fun daysStayExactUnderTwoWeeks() {
        assertEquals(ExpirySpan.Days(1), expirySpan(1))
        assertEquals(ExpirySpan.Days(5), expirySpan(5))
        assertEquals(ExpirySpan.Days(13), expirySpan(13))
    }

    @Test
    fun weeksRoundToNearestUpToTwoMonths() {
        assertEquals(ExpirySpan.Weeks(2), expirySpan(14))
        assertEquals(ExpirySpan.Weeks(2), expirySpan(17))
        assertEquals(ExpirySpan.Weeks(3), expirySpan(18))
        assertEquals(ExpirySpan.Weeks(3), expirySpan(20))
        assertEquals(ExpirySpan.Weeks(4), expirySpan(30))
        assertEquals(ExpirySpan.Weeks(8), expirySpan(59))
    }

    @Test
    fun monthsRoundToNearestUnderAYear() {
        assertEquals(ExpirySpan.Months(2), expirySpan(60))
        assertEquals(ExpirySpan.Months(5), expirySpan(145))
        assertEquals(ExpirySpan.Months(6), expirySpan(180))
        assertEquals(ExpirySpan.Months(11), expirySpan(334))
        assertEquals(ExpirySpan.Months(11), expirySpan(350))
    }

    @Test
    fun yearsFromAboutElevenAndAHalfMonths() {
        assertEquals(ExpirySpan.Years(1), expirySpan(351))
        assertEquals(ExpirySpan.Years(1), expirySpan(365))
        assertEquals(ExpirySpan.Years(1), expirySpan(547))
        assertEquals(ExpirySpan.Years(2), expirySpan(548))
        assertEquals(ExpirySpan.Years(2), expirySpan(730))
    }
}
