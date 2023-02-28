package com.oursky.authgear.latte

import kotlinx.serialization.Serializable

@Serializable
data class LatteViewPageEvent(
    var path: String
)
