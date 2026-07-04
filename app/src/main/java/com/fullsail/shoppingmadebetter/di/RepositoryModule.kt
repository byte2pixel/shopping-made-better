package com.fullsail.shoppingmadebetter.di

import com.fullsail.shoppingmadebetter.data.StoreRepository
import com.fullsail.shoppingmadebetter.data.repository.impl.StoreRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces to their Supabase-backed implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository
}
