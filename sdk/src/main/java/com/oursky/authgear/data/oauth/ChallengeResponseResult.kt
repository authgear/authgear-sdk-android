package com.oursky.authgear.data.oauth

import com.oursky.authgear.oauth.ChallengeResponse
import kotlinx.serialization.Serializable

@Serializable
internal data class ChallengeResponseResult(
    val result: ChallengeResponse
)