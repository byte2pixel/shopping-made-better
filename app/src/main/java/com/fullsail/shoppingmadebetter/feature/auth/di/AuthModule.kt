package com.fullsail.shoppingmadebetter.feature.auth.di

import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepository
import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.auth.domain.SignInUseCase
import com.fullsail.shoppingmadebetter.feature.auth.domain.SignInUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.auth.domain.SignUpUseCase
import com.fullsail.shoppingmadebetter.feature.auth.domain.SignUpUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the auth feature's repository and use-case interfaces to their impls. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindSignInUseCase(impl: SignInUseCaseImpl): SignInUseCase

    @Binds
    abstract fun bindSignUpUseCase(impl: SignUpUseCaseImpl): SignUpUseCase
}