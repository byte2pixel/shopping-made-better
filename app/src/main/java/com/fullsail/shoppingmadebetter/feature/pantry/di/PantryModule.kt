package com.fullsail.shoppingmadebetter.feature.pantry.di

import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryPreferencesRepository
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryPreferencesRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustmentUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiryUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryQuantityUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryQuantityUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PantryModule {
    @Binds @Singleton
    abstract fun bindPantryRepository(impl: PantryRepositoryImpl): PantryRepository

    @Binds @Singleton
    abstract fun bindPantryPreferencesRepository(
        impl: PantryPreferencesRepositoryImpl,
    ): PantryPreferencesRepository

    @Binds
    abstract fun bindGetInventoryUseCase(impl: GetInventoryUseCaseImpl): GetInventoryUseCase

    @Binds
    abstract fun bindDeleteInventoryItemUseCase(impl: DeleteInventoryItemUseCaseImpl): DeleteInventoryItemUseCase

    @Binds
    abstract fun bindGetSkipRemoveConfirmationUseCase(
        impl: GetSkipRemoveConfirmationUseCaseImpl,
    ): GetSkipRemoveConfirmationUseCase

    @Binds
    abstract fun bindSetSkipRemoveConfirmationUseCase(
        impl: SetSkipRemoveConfirmationUseCaseImpl,
    ): SetSkipRemoveConfirmationUseCase

    @Binds
    abstract fun bindUpdateInventoryQuantityUseCase(
        impl: UpdateInventoryQuantityUseCaseImpl,
    ): UpdateInventoryQuantityUseCase

    @Binds
    abstract fun bindUpdateInventoryLocationUseCase(
        impl: UpdateInventoryLocationUseCaseImpl,
    ): UpdateInventoryLocationUseCase

    @Binds
    abstract fun bindUpdateInventoryExpiryUseCase(
        impl: UpdateInventoryExpiryUseCaseImpl,
    ): UpdateInventoryExpiryUseCase

    @Binds
    abstract fun bindUpdateInventoryLowStockThresholdUseCase(
        impl: UpdateInventoryLowStockThresholdUseCaseImpl,
    ): UpdateInventoryLowStockThresholdUseCase

    @Binds
    abstract fun bindApplyInventoryAdjustmentUseCase(
        impl: ApplyInventoryAdjustmentUseCaseImpl,
    ): ApplyInventoryAdjustmentUseCase
}
