package com.fullsail.shoppingmadebetter.feature.shoppinglists.di

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.insertItem.InsertItemRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.insertItem.InsertItemRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.productSearch.ProductSearchRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.productSearch.ProductSearchRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.shoppingTrip.ShoppingTripRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.shoppingTrip.ShoppingTripRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.storeProductPricing.StoreProductPricingRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.storeProductPricing.StoreProductPricingRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricingUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricingUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ShoppingListsModule {

    @Binds
    @Singleton
    abstract fun bindShoppingTripRepository(
        impl: ShoppingTripRepositoryImpl,
    ): ShoppingTripRepository

    @Binds
    abstract fun bindGetShoppingTripsUseCase(
        impl: GetShoppingTripsUseCaseImpl,
    ): GetShoppingTripsUseCase

    @Binds
    @Singleton
    abstract fun bindStoreProductPricingRepository(
        impl: StoreProductPricingRepositoryImpl,
    ): StoreProductPricingRepository

    @Binds
    abstract fun bindStoreProductPricingUseCase(
        impl: StoreProductPricingUseCaseImpl,
    ): StoreProductPricingUseCase
    @Binds
    @Singleton
    abstract fun bindProductSearchRepository(
        impl: ProductSearchRepositoryImpl,
    ): ProductSearchRepository

    @Binds
    abstract fun bindProductSearchUseCase(
        impl: ProductSearchUseCaseImpl,
    ): ProductSearchUseCase
    @Binds
    @Singleton
    abstract fun bindInsertItemRepository(
        impl: InsertItemRepositoryImpl,
    ): InsertItemRepository

    @Binds
    abstract fun bindInsertItemUseCase(
        impl: InsertItemUseCaseImpl,
    ): InsertItemUseCase
}

