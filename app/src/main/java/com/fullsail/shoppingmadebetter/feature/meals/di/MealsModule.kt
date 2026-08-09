package com.fullsail.shoppingmadebetter.feature.meals.di

import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.meals.domain.SelectMealUseCase
import com.fullsail.shoppingmadebetter.feature.meals.domain.SelectMealUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class MealsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindMealsRepository(
        impl: MealsRepositoryImpl
    ): MealsRepository

    @Binds
    @ViewModelScoped
    abstract fun bindSelectMealUseCase(
        impl: SelectMealUseCaseImpl
    ): SelectMealUseCase
}