package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfoAddress(
    val formatted: String? = null,
    @SerialName("street_address")
    val streetAddress: String? = null,
    val locality: String? = null,
    val region: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    val country: String? = null
)
