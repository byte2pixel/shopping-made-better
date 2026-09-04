package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** The loaded pantry to scan for zero-stock estimates. */
data class GetPantryEstimateAlerts(val productGroups: List<ProductGroup>)

/**
 * Lots at zero whose latest audit row is `auto`, in display order. Dismissed lots and
 * low-stock lots (the digest's job) are excluded. Pure: no repository, no failure path.
 */
interface GetPantryEstimateAlertsUseCase : UseCase<GetPantryEstimateAlerts, List<InventoryItem>>
