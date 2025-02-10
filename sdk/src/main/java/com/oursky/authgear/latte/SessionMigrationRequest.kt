package com.oursky.authgear.latte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionMigrationRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("access_token")
    val accessToken: String,
)
