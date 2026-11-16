package com.oursky.authgear.net

internal fun Map<String, String>.toFormData(): String {
    return entries.joinToString("&") { (key, value) ->
        "${encodeFormComponent(key)}=${encodeFormComponent(value)}"
    }
}
