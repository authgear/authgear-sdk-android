package com.oursky.authgear.net

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Encode as a URI query component (RFC 3986), so space becomes %20.
internal fun Map<String, String>.toQueryParameter(): String {
    return entries.joinToString("&") { (key, value) ->
        "${encodeQueryComponent(key)}=${encodeQueryComponent(value)}"
    }
}

// URLEncoder encodes a literal "+" as "%2B", so any "+" in its output came from a space.
private fun encodeQueryComponent(s: String): String {
    return encodeFormComponent(s).replace("+", "%20")
}

// Encode as application/x-www-form-urlencoded, so space becomes +.
internal fun encodeFormComponent(s: String): String {
    return URLEncoder.encode(s, StandardCharsets.UTF_8.name())
}
