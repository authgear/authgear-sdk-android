package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class WebSocketMessage(
    @SerialName("kind")
    val kind: WebSocketMessageKind,

    @SerialName("data")
    val data: JsonObject?
)
