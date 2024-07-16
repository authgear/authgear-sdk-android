package com.oursky.authgear

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.charset.Charset
import java.security.Signature
import java.time.Instant

internal enum class JWTHeaderType(val value: String) {
    ANONYMOUS("vnd.authgear.anonymous-request"),
    BIOMETRIC("vnd.authgear.biometric-request"),
    APP2APP("vnd.authgear.app2app-request"),
    DPOPJWT("dpop+jwt")
}

internal data class JWTHeader(
    val typ: JWTHeaderType,
    val kid: String,
    val alg: String,
    val jwk: JWK?
)

internal fun JWTHeader.toJsonObject(): JsonObject {
    val header = mutableMapOf<String, JsonElement>()
    header["typ"] = JsonPrimitive(typ.value)
    header["kid"] = JsonPrimitive(kid)
    header["alg"] = JsonPrimitive(alg)
    jwk?.let { jwk ->
        header["jwk"] = jwk.toJsonObject()
    }
    return JsonObject(header)
}

internal data class JWTPayload(
    val iat: Long,
    val exp: Long,
    val jti: String? = null,
    val htm: String? = null,
    val htu: String? = null,
    val challenge: String? = null,
    val action: String? = null,
    val deviceInfo: DeviceInfoRoot? = null
) {
    constructor(now: Instant, challenge: String, action: String, deviceInfo: DeviceInfoRoot? = null) : this(
        iat = now.epochSecond,
        exp = now.epochSecond + 60,
        challenge = challenge,
        action = action,
        deviceInfo = deviceInfo
    )

    constructor(now: Instant, jti: String, htu: String, htm: String) : this(
        iat = now.epochSecond,
        exp = now.epochSecond + 60,
        jti = jti,
        htu = htu,
        htm = htm
    )
}

internal fun JWTPayload.toJsonObject(): JsonObject {
    val m = mutableMapOf<String, JsonElement>()
    m["iat"] = JsonPrimitive(iat)
    m["exp"] = JsonPrimitive(exp)
    m["challenge"] = JsonPrimitive(challenge)
    m["action"] = JsonPrimitive(action)
    if (deviceInfo != null) {
        m["device_info"] = deviceInfo.toJsonObject()
    }
    return JsonObject(m)
}

internal fun signJWT(signature: Signature, header: JWTHeader, payload: JWTPayload): String {
    val data = "${base64UrlEncode(Json.encodeToString(header.toJsonObject()).toUTF8())}.${base64UrlEncode(Json.encodeToString(payload.toJsonObject()).toUTF8())}"
    signature.update(data.toUTF8())
    val sig = signature.sign()
    return "$data.${base64UrlEncode(sig)}"
}

internal fun decodeJWT(jwt: String): JsonObject {
    val parts = jwt.split(".")
    if (parts.size != 3) {
        throw AuthgearException("invalid jwt: $jwt")
    }
    val base64UrlEncoded = parts[1]
    val bytes = bae64UrlDecode(base64UrlEncoded)
    val utf8 = String(bytes, Charset.forName("UTF-8"))
    val jsonElement = Json.parseToJsonElement(utf8)
    return jsonElement.jsonObject
}
