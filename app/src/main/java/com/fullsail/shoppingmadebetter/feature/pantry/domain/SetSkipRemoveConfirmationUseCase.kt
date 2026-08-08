package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Persists the user's "don't ask again" choice for the remove-from-pantry
 * confirmation dialog. `true` suppresses the dialog on future removals.
 */
interface SetSkipRemoveConfirmationUseCase : UseCase<Boolean, Unit>