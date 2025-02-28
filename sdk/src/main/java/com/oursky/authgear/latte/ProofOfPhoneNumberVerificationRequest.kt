package com.oursky.authgear.latte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProofOfPhoneNumberVerificationRequest(
    @SerialName("access_token")
    val accessToken: String,
)
