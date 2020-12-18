package com.oursky.authgear.data.oauth

import com.oursky.authgear.oauth.AppSessionTokenResponse
import kotlinx.serialization.Serializable

@Serializable
internal data class AppSessionTokenResponseResult(
    val result: AppSessionTokenResponse
)