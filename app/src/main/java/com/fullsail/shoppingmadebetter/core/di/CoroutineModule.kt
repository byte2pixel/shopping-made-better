package com.fullsail.shoppingmadebetter.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Provides the [ApplicationScope] coroutine scope. A [SupervisorJob] keeps one
 * failed child from cancelling the rest, and [Dispatchers.Default] backs the
 * background work. The scope is never cancelled — it lives for the process.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}