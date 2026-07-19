package com.fullsail.shoppingmadebetter.feature.onboarding.di

import com.fullsail.shoppingmadebetter.feature.onboarding.data.OnboardingRepository
import com.fullsail.shoppingmadebetter.feature.onboarding.data.OnboardingRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferencesUseCase
import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferencesUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class OnboardingModule {

    @Binds
    @ViewModelScoped
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): OnboardingRepository

    @Binds
    @ViewModelScoped
    abstract fun bindSavePreferencesUseCase(
        impl: SavePreferencesUseCaseImpl
    ): SavePreferencesUseCase
}