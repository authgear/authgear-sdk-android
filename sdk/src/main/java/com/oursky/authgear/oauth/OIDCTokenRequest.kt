package com.oursky.authgear.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OIDCTokenRequest(
    // TODO: Make it an enum
    // Research if kotlinx has better support of enum now
    @SerialName("grant_type")
    val grantType: String,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("redirect_uri")
    val redirectUri: String? = null,
    val code: String? = null,
    @SerialName("code_verifier")
    val codeVerifier: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    val jwt: String? = null
)
