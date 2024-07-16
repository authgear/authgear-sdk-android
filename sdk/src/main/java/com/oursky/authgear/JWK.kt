package com.oursky.authgear

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

@OptIn(ExperimentalEncodingApi::class)
internal fun JWK.toSHA256Thumbprint(): String {
    val p = mutableMapOf<String, String>()
    when (kty) {
        "RSA" -> {
            // required members for an RSA public key are e, kty, n
            // in lexicographic order
            p["e"] = e
            p["kty"] = kty
            p["n"] = n
        }
        else -> {
            throw NotImplementedError("unknown kty")
        }
    }
    val jsonBytes = Json.encodeToString(p).toByteArray()
    val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(jsonBytes)

    return Base64.UrlSafe.encode(hashBytes).removeSuffix("=")
}

internal fun publicKeyToJWK(kid: String, publicKey: PublicKey): JWK {
    val rsaPublicKey = publicKey as RSAPublicKey
    return JWK(
        kid = kid,
        n = base64UrlEncode(rsaPublicKey.modulus.toByteArray()),
        e = base64UrlEncode(rsaPublicKey.publicExponent.toByteArray())
    )
}