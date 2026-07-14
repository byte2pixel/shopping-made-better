package com.fullsail.shoppingmadebetter.feature.pantry.di

import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCaseImpl
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

    @Binds
    abstract fun bindGetInventoryUseCase(impl: GetInventoryUseCaseImpl): GetInventoryUseCase

    @Binds
    abstract fun bindGetInventoryItemUseCase(impl: GetInventoryItemUseCaseImpl): GetInventoryItemUseCase
}
