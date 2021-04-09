package com.oursky.authgear.oauth

import com.oursky.authgear.GrantType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OIDCTokenRequest(
    @SerialName("grant_type")
    val grantType: GrantType,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("x_device_info")
    val xDeviceInfo: String,
    @SerialName("redirect_uri")
    val redirectUri: String? = null,
    val code: String? = null,
    @SerialName("code_verifier")
    val codeVerifier: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    val jwt: String? = null
)
