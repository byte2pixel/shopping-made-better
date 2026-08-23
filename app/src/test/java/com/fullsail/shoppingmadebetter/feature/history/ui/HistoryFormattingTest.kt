package com.fullsail.shoppingmadebetter.feature.history.ui

import kotlinx.datetime.LocalDate
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.util.Locale

/** Unit tests for the History tab's display formatting. */
class HistoryFormattingTest {

    @Test
    fun `formatQuantity drops the trailing zeros of a whole quantity`() {
        // The column is numeric(10,3), so 2 arrives as 2.000.
        assertEquals("2", formatQuantity(2.0))
        assertEquals("10", formatQuantity(10.0))
    }

    @Test
    fun `formatQuantity keeps a fractional quantity`() {
        assertEquals("1.5", formatQuantity(1.5))
        assertEquals("0.75", formatQuantity(0.75))
    }

    @Test
    fun `formatQuantity keeps a large quantity in plain notation`() {
        // stripTrailingZeros alone would render this as 6E+2.
        assertEquals("600", formatQuantity(600.0))
    }

    @Test
    fun `formatPrice always shows two decimals in dollars`() {
        assertEquals("$12.50", formatPrice(12.5))
        assertEquals("$0.00", formatPrice(0.0))
        assertEquals("$5.22", formatPrice(5.22))
    }

    @Test
    fun `formatTripDate renders a medium localized date`() {
        assertEquals("Aug 19, 2026", formatTripDate(LocalDate(2026, 8, 19)))
    }

    companion object {
        private val defaultLocale: Locale = Locale.getDefault()

        /**
         * Trip dates deliberately follow the device locale, and the date formatter bakes
         * that locale in when it is first created — so pin the default before any test
         * touches it. Without this the date assertion depends on the machine running it.
         */
        @BeforeClass
        @JvmStatic
        fun pinLocale() {
            Locale.setDefault(Locale.US)
        }

        @AfterClass
        @JvmStatic
        fun restoreLocale() {
            Locale.setDefault(defaultLocale)
        }
    }
}
