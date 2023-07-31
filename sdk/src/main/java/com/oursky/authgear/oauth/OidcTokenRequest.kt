package com.oursky.authgear.oauth

import com.oursky.authgear.GrantType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OidcTokenRequest(
    @SerialName("grant_type")
    val grantType: GrantType,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("x_device_info")
    val xDeviceInfo: String? = null,
    @SerialName("redirect_uri")
    val redirectUri: String? = null,
    val code: String? = null,
    @SerialName("code_verifier")
    val codeVerifier: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("jwt")
    val jwt: String? = null,
    @SerialName("x_app2app_device_key_jwt")
    val xApp2AppDeviceKeyJwt: String? = null,
    @SerialName("code_challenge")
    val codeChallenge: String? = null,
    @SerialName("code_challenge_method")
    val codeChallengeMethod: String? = null
)
