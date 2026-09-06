package com.fullsail.shoppingmadebetter.feature.shoppinglists.di

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetItemDetailsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetItemDetailsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetShoppingListItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetShoppingListItemsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RemoveListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RemoveListUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RenameShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RenameShoppingListUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.UpdateQuantityUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.UpdateQuantityUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.isCheckedUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.isCheckedUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CheckAllItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CheckAllItemsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CompleteShoppingTripUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CompleteShoppingTripUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricingUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricingUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Suppress("UNUSED_PARAMETER")
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
    abstract fun bindInsertShoppingList(
        impl: ShoppingListUseCaseImpl,
    ): ShoppingListUseCase

    @Binds
    abstract fun bindQuantityUpdate(
        impl: UpdateQuantityUseCaseImpl,
    ): UpdateQuantityUseCase
    @Binds
    abstract fun bindItemDetail(
        impl: GetItemDetailsUseCaseImpl,
    ): GetItemDetailsUseCase

    @Binds
    abstract fun bindRenameShoppingList(
        impl: RenameShoppingListUseCaseImpl,
    ): RenameShoppingListUseCase
    @Binds
    abstract fun bindCheckItem(
        impl: isCheckedUseCaseImpl,
    ): isCheckedUseCase


    @Binds
    abstract fun bindStoreProductPricingUseCase(
        impl: StoreProductPricingUseCaseImpl,
    ): StoreProductPricingUseCase
    @Binds
    abstract fun bindShoppingListItemsUseCase(
        impl: GetShoppingListItemsUseCaseImpl,
    ): GetShoppingListItemsUseCase
    @Binds
    abstract fun bindDeleteItemUseCase(
        impl: DeleteItemsUseCaseImpl,
    ): DeleteItemsUseCase
    @Binds
    abstract fun bindRemoveListUseCase(
        impl: RemoveListUseCaseImpl,
    ): RemoveListUseCase


    @Binds
    abstract fun bindProductSearchUseCase(
        impl: ProductSearchUseCaseImpl,
    ): ProductSearchUseCase


    @Binds
    abstract fun bindInsertItemUseCase(
        impl: InsertItemUseCaseImpl,
    ): InsertItemUseCase

    @Binds
    abstract fun bindCompleteShoppingTripUseCase(
        impl: CompleteShoppingTripUseCaseImpl,
    ): CompleteShoppingTripUseCase

    @Binds
    abstract fun bindCheckAllItemsUseCase(
        impl: CheckAllItemsUseCaseImpl,
    ): CheckAllItemsUseCase
}

