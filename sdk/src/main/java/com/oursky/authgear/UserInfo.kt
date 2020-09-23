package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String,
    @SerialName("https://authgear.com/claims/user/is_verified")
    val isVerified: Boolean,
    @SerialName("https://authgear.com/claims/user/is_anonymous")
    val isAnonymous: Boolean
)
