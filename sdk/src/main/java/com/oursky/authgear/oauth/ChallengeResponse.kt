package com.oursky.authgear.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChallengeResponse(
    val token: String,
    @SerialName("expire_at")
    val expireAt: String
)
