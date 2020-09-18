package com.oursky.authgear

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String
)
