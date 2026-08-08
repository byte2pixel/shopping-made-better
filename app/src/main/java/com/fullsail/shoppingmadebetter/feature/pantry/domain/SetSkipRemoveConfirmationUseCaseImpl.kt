package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryPreferencesRepository
import javax.inject.Inject

class SetSkipRemoveConfirmationUseCaseImpl @Inject constructor(
    private val pantryPreferencesRepository: PantryPreferencesRepository,
) : SetSkipRemoveConfirmationUseCase {
    override suspend fun execute(input: Boolean) =
        pantryPreferencesRepository.setSkipRemoveConfirmation(input)
}