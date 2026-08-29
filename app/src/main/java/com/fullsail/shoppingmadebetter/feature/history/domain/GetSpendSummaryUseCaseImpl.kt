package com.fullsail.shoppingmadebetter.feature.history.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.history.data.SpendRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import javax.inject.Inject

class GetSpendSummaryUseCaseImpl @Inject constructor(
    private val spendRepository: SpendRepository,
    private val clock: Clock,
) : GetSpendSummaryUseCase {
    override suspend fun execute(input: Unit): GetSpendSummaryUseCase.Output = try {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val thisMonth = today.startOfMonth()
        val lastMonth = today.previousMonthStart()

        val months = spendRepository.getSpendByMonth(
            // This month is one of the six, so the window opens five back.
            sinceMonth = thisMonth.minus(DatePeriod(months = MONTHS_BACK - 1)),
        )
        // The savings card names its own window, so it need not match the calendar
        // months the other two cards speak in.
        val costs = spendRepository.getTripCostsSince(
            from = today.minus(DatePeriod(days = SAVINGS_WINDOW_DAYS - 1)),
        )

        GetSpendSummaryUseCase.Output.Success(
            SpendSummary(
                thisMonth = months.monthTotal(thisMonth),
                lastMonth = months.monthTotal(lastMonth).takeIf { it.tripCount > 0 },
                byStore = months.storeBreakdown(thisMonth),
                cheapest = cheapestStore(costs),
            ),
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch spend summary: ${e.message}", e)
        GetSpendSummaryUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "GetSpendSummaryUseCase"

        /** Enough history for this month and last, with room to grow a trend view. */
        const val MONTHS_BACK = 6

        /** The savings card's window; today counts as one of the days. */
        const val SAVINGS_WINDOW_DAYS = 30
    }
}
