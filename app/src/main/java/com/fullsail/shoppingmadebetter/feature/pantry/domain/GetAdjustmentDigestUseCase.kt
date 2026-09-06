package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetAdjustmentDigestUseCase : UseCase<Unit, GetAdjustmentDigestUseCase.Output> {
    sealed interface Output {
        /** This week's automatic adjustments, newest first. */
        data class Success(val entries: List<AdjustmentDigestEntry>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
