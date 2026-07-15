package com.fullsail.shoppingmadebetter.feature.shoppinglists.di

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepositoryImpl
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
        impl: ShoppingListRepositoryImpl,
    ): ShoppingListRepository

    @Binds
    abstract fun bindGetShoppingTripsUseCase(
        impl: GetShoppingTripsUseCaseImpl,
    ): GetShoppingTripsUseCase

    @Binds
    abstract fun bindStoreProductPricingUseCase(
        impl: StoreProductPricingUseCaseImpl,
    ): StoreProductPricingUseCase


    @Binds
    abstract fun bindProductSearchUseCase(
        impl: ProductSearchUseCaseImpl,
    ): ProductSearchUseCase


    @Binds
    abstract fun bindInsertItemUseCase(
        impl: InsertItemUseCaseImpl,
    ): InsertItemUseCase
}

