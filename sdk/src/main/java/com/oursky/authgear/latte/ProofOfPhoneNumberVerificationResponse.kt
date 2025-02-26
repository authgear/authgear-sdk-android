package com.oursky.authgear.latte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProofOfPhoneNumberVerificationResponse(
    @SerialName("proof_of_phone_number_verification")
    val proofOfPhoneNumberVerification: String,
)

