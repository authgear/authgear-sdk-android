package com.oursky.authgear.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OIDCConfiguration(
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("userinfo_endpoint")
    val userInfoEndpoint: String,
    @SerialName("revocation_endpoint")
    val revocationEndpoint: String,
    @SerialName("end_session_endpoint")
    val endSessionEndpoint: String
)
