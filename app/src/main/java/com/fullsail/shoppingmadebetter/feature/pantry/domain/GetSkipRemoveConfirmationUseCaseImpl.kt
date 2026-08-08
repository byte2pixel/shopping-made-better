package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetSkipRemoveConfirmationUseCaseImpl @Inject constructor(
    private val pantryPreferencesRepository: PantryPreferencesRepository,
) : GetSkipRemoveConfirmationUseCase {
    override suspend fun execute(input: Unit): Boolean =
        pantryPreferencesRepository.skipRemoveConfirmation().first()
}