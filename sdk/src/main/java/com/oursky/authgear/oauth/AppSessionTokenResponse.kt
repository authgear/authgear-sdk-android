package com.oursky.authgear.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AppSessionTokenResponse(
    @SerialName("app_session_token")
    val appSessionToken: String,
    @SerialName("expire_at")
    val expireAt: String
)
