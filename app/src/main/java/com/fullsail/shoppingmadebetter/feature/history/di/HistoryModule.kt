package com.fullsail.shoppingmadebetter.feature.history.di

import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {
    @Binds @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    abstract fun bindGetPurchaseHistoryUseCase(
        impl: GetPurchaseHistoryUseCaseImpl,
    ): GetPurchaseHistoryUseCase

    @Binds
    abstract fun bindGetPurchaseTripUseCase(
        impl: GetPurchaseTripUseCaseImpl,
    ): GetPurchaseTripUseCase
}
