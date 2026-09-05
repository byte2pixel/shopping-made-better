package com.fullsail.shoppingmadebetter.feature.pantry.domain

import javax.inject.Inject

class GetPantryEstimateAlertsUseCaseImpl @Inject constructor() : GetPantryEstimateAlertsUseCase {
    override suspend fun execute(input: GetPantryEstimateAlerts): List<InventoryItem> =
        input.productGroups
            .flatMap { it.lots }
            .filter { it.lastAdjustmentReason == AdjustmentReason.Auto && it.quantity <= 0 }
}
