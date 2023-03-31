package com.oursky.authgear.latte

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LatteTrackingEvent(
    var event_name: String,
    var params: JsonObject
)
