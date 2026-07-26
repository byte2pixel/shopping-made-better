package com.fullsail.shoppingmadebetter.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock
import javax.inject.Singleton

/**
 * Time provider that injects system clock for the production code
 * but allows injecting for unit tests to have a deterministic time.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}