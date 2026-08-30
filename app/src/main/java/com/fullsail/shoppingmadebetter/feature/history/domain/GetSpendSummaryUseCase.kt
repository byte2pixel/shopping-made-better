package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetSpendSummaryUseCase : UseCase<Unit, GetSpendSummaryUseCase.Output> {
    sealed interface Output {
        /** [summary] may be empty; the tab hides the section rather than showing zeros. */
        data class Success(val summary: SpendSummary) : Output

        data class Failure(val error: Throwable) : Output
    }
}
