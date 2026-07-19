package com.fullsail.shoppingmadebetter.feature.profile.di

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.profile.domain.ChangePasswordUseCase
import com.fullsail.shoppingmadebetter.feature.profile.domain.ChangePasswordUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ProfileModule {

    @Binds
    @ViewModelScoped
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @ViewModelScoped
    abstract fun bindChangePasswordUseCase(
        impl: ChangePasswordUseCaseImpl
    ): ChangePasswordUseCase
}