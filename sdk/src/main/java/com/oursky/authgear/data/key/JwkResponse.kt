package com.oursky.authgear.data.key

data class JwkResponse(
    val kid: String,
    val alg: String,
    val jwk: Jwk?
)
