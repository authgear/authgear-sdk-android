package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Authenticator(
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val type: AuthenticatorType,
    val kind: AuthenticatorKind
)
