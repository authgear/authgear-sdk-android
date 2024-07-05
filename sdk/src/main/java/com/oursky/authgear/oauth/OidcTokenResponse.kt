package com.oursky.authgear.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OidcTokenResponse(
    @SerialName("id_token")
    val idToken: String? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("code")
    val code: String? = null,
    @SerialName("device_secret")
    val deviceSecret: String? = null
)
