package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.fullsail.shoppingmadebetter.R
import kotlin.math.roundToInt

/** Days until expiry as the unit a person would say it in. See [expirySpan]. */
sealed interface ExpirySpan {
    data object Expired : ExpirySpan
    data object Today : ExpirySpan
    data class Days(val count: Int) : ExpirySpan
    data class Weeks(val count: Int) : ExpirySpan
    data class Months(val count: Int) : ExpirySpan
    data class Years(val count: Int) : ExpirySpan
}

private const val DAYS_PER_WEEK = 7.0
private const val DAYS_PER_MONTH = 30.44
private const val DAYS_PER_YEAR = 365.25

/** Exact days up to this many; weeks from here. Covers every color threshold. */
private const val MAX_EXACT_DAYS = 13

/** Weeks up to this many; months from here (about two months). */
private const val MAX_WEEKS = 8

private const val MAX_MONTHS = 11

/**
 * Turns [days] until expiry into the nearest whole unit: exact days under two weeks, then
 * weeks, months and years, each rounded to nearest. Thresholds that drive color and the
 * Expiring filter keep reading raw days and never go through this.
 */
fun expirySpan(days: Int): ExpirySpan {
    if (days < 0) return ExpirySpan.Expired
    if (days == 0) return ExpirySpan.Today
    if (days <= MAX_EXACT_DAYS) return ExpirySpan.Days(days)
    val weeks = (days / DAYS_PER_WEEK).roundToInt()
    if (weeks <= MAX_WEEKS) return ExpirySpan.Weeks(weeks)
    val months = (days / DAYS_PER_MONTH).roundToInt()
    if (months <= MAX_MONTHS) return ExpirySpan.Months(months)
    return ExpirySpan.Years(maxOf(1, (days / DAYS_PER_YEAR).roundToInt()))
}

/** Long form: "Expired", "Today", "9 days", "3 weeks", "6 months", "2 years". */
@Composable
fun expirySpanLabel(span: ExpirySpan): String = when (span) {
    ExpirySpan.Expired -> stringResource(R.string.pantry_expiry_expired)
    ExpirySpan.Today -> stringResource(R.string.pantry_expiry_today)
    is ExpirySpan.Days -> pluralStringResource(R.plurals.pantry_expiry_days, span.count, span.count)
    is ExpirySpan.Weeks -> pluralStringResource(R.plurals.expiry_span_weeks, span.count, span.count)
    is ExpirySpan.Months -> pluralStringResource(R.plurals.expiry_span_months, span.count, span.count)
    is ExpirySpan.Years -> pluralStringResource(R.plurals.expiry_span_years, span.count, span.count)
}

/** Chip form: "Expired", "Today", "9d", "3w", "6m", "2y". */
@Composable
fun expirySpanShortLabel(span: ExpirySpan): String = when (span) {
    ExpirySpan.Expired -> stringResource(R.string.pantry_expiry_expired)
    ExpirySpan.Today -> stringResource(R.string.pantry_expiry_today)
    is ExpirySpan.Days -> stringResource(R.string.pantry_expiry_in_days_short, span.count)
    is ExpirySpan.Weeks -> stringResource(R.string.expiry_span_weeks_short, span.count)
    is ExpirySpan.Months -> stringResource(R.string.expiry_span_months_short, span.count)
    is ExpirySpan.Years -> stringResource(R.string.expiry_span_years_short, span.count)
}

/** Spoken form: "Expired", "Expires today", "9 days left", "3 weeks left", and so on. */
@Composable
fun expirySpanDescription(span: ExpirySpan): String = when (span) {
    ExpirySpan.Expired -> stringResource(R.string.pantry_detail_expired)
    ExpirySpan.Today -> stringResource(R.string.pantry_detail_expires_today)
    is ExpirySpan.Days ->
        pluralStringResource(R.plurals.pantry_detail_expires_in_days, span.count, span.count)
    is ExpirySpan.Weeks -> pluralStringResource(R.plurals.expiry_span_weeks_left, span.count, span.count)
    is ExpirySpan.Months -> pluralStringResource(R.plurals.expiry_span_months_left, span.count, span.count)
    is ExpirySpan.Years -> pluralStringResource(R.plurals.expiry_span_years_left, span.count, span.count)
}
