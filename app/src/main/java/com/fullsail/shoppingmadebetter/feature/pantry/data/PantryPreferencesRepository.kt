package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.coroutines.flow.Flow

/** Local, device-level UX preferences for the pantry feature. */
interface PantryPreferencesRepository {
    /**
     * Whether the user opted to skip the remove-from-pantry confirmation dialog
     * (the "don't ask again" choice). Defaults to `false`.
     */
    fun skipRemoveConfirmation(): Flow<Boolean>

    /** Persists the user's "don't ask again" choice for the remove dialog. */
    suspend fun setSkipRemoveConfirmation(skip: Boolean)
}