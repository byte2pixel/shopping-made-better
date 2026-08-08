package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Reads whether the remove-from-pantry confirmation dialog should be skipped
 * (the user's "don't ask again" choice). Returns `false` when unset.
 */
interface GetSkipRemoveConfirmationUseCase : UseCase<Unit, Boolean>