package com.fullsail.shoppingmadebetter.feature.profile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `profiles.auto_adjust_enabled` flag for one user. */
@Serializable
data class AutoAdjustSettingDto(
    @SerialName("auto_adjust_enabled") val autoAdjustEnabled: Boolean,
)
