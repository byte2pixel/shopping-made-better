package com.fullsail.shoppingmadebetter.feature.product.di

import com.fullsail.shoppingmadebetter.feature.product.data.ProductRepository
import com.fullsail.shoppingmadebetter.feature.product.data.ProductRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.product.domain.GetProductDetailUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.GetProductDetailUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductModule {
    @Binds @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    abstract fun bindGetProductDetailUseCase(
        impl: GetProductDetailUseCaseImpl,
    ): GetProductDetailUseCase
}
