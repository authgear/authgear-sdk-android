package com.oursky.authgear

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey

internal data class JWK(
    val kid: String,
    val alg: String = "RS256",
    val kty: String = "RSA",
    val n: String,
    val e: String
)

internal fun JWK.toJsonObject(): JsonObject {
    val m = mutableMapOf<String, JsonElement>()
    m["kid"] = JsonPrimitive(kid)
    m["alg"] = JsonPrimitive(alg)
    m["kty"] = JsonPrimitive(kty)
    m["n"] = JsonPrimitive(n)
    m["e"] = JsonPrimitive(e)
    return JsonObject(m)
}

internal fun publicKeyToJWK(kid: String, publicKey: PublicKey): JWK {
    val rsaPublicKey = publicKey as RSAPublicKey
    return JWK(
        kid = kid,
        n = base64UrlEncode(rsaPublicKey.modulus.toByteArray()),
        e = base64UrlEncode(rsaPublicKey.publicExponent.toByteArray())
    )
}