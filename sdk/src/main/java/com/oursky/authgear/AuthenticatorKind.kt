package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AuthenticatorKind {
    @SerialName("primary")
    PRIMARY,
    @SerialName("secondary")
    SECONDARY
}
