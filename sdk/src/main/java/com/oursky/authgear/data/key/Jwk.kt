package com.oursky.authgear.data.key

internal data class Jwk(
    val kid: String,
    val kty: String,
    val n: String,
    val e: String
)
