package com.fullsail.shoppingmadebetter.feature.stores.di

import com.fullsail.shoppingmadebetter.feature.stores.data.StoreRepository
import com.fullsail.shoppingmadebetter.feature.stores.data.StoreRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the stores feature's repository and use-case interfaces to their impls. */
@Module
@InstallIn(SingletonComponent::class)
abstract class StoresModule {

    @Binds
    @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository

    @Binds
    abstract fun bindGetStoresUseCase(impl: GetStoresUseCaseImpl): GetStoresUseCase
}
