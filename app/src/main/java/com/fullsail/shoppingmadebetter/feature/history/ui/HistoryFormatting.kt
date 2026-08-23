package com.fullsail.shoppingmadebetter.feature.history.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Neither formatter is thread-safe; both are only read from the composition (main thread).
private val currencyFormat: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale.US) }
private val tripDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/**
 * Money as US dollars, e.g. `$5.22`. The seeded pricing is in dollars, so the
 * device locale must not relabel it as another currency.
 */
internal fun formatPrice(amount: Double): String = currencyFormat.format(amount)

/**
 * A purchased quantity without trailing zeros: `2.000` reads as "2", `1.500` as
 * "1.5". The column is `numeric(10,3)`, so whole quantities would otherwise show
 * three decimal places.
 */
internal fun formatQuantity(quantity: Double): String =
    BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString()

/** A trip's date in the device's medium localized form, e.g. "Aug 19, 2026". */
internal fun formatTripDate(date: LocalDate): String =
    date.toJavaLocalDate().format(tripDateFormat)
