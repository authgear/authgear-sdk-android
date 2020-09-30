package com.oursky.authgear.data.key

internal data class JwkResponse(
    val kid: String,
    val alg: String,
    val jwk: Jwk?
)
