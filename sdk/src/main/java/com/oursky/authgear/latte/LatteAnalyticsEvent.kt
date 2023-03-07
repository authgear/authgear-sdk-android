package com.oursky.authgear.latte

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class LatteAnalyticsEvent(
    var type: String,
    var path: String,
    var url: String,
    var clientID: String,
    var data: JsonObject?
)
