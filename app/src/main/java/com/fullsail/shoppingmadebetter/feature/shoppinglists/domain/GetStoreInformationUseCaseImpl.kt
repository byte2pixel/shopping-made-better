package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.StoreAddressInformationDto
import javax.inject.Inject

class GetStoreInformationUseCaseImpl @Inject constructor(
private val repository: ShoppingListRepository,
) : GetStoreInformationUseCase {
    override suspend fun execute(input: String): GetStoreInformationUseCase.Output {
        return try {
            GetStoreInformationUseCase.Output.Success(repository.getStoreAddress(input).toDomain())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to grab product information: ${e.message}", e)
            GetStoreInformationUseCase.Output.Failure(e)
        }
    }

    private fun StoreAddressInformationDto.toDomain() = StoreAddressInformation(
        storeId = storeId,
        name = name,
        address = address,
        city = city,
        state = state,
        postalCode = postalCode,
        phone = phone


    )



    private companion object {
        const val TAG = "getStoreAddressInformation"
    }
}