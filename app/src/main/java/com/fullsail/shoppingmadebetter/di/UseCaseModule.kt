package com.fullsail.shoppingmadebetter.di

import com.fullsail.shoppingmadebetter.domain.usecase.GetStoresUseCase
import com.fullsail.shoppingmadebetter.domain.usecase.impl.GetStoresUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds use-case interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindGetStoresUseCase(impl: GetStoresUseCaseImpl): GetStoresUseCase
}
