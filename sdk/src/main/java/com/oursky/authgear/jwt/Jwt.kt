package com.oursky.authgear.jwt

import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.charset.StandardCharsets

private fun encodeBase64(input: String): String {
    return Base64.encodeToString(input.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
}

internal fun prepareJwtData(
    header: Map<String, JsonElement>,
    payload: Map<String, String>
): String {
    return "${encodeBase64(Json.encodeToString(header))}.${encodeBase64(Json.encodeToString(payload))}"
}
