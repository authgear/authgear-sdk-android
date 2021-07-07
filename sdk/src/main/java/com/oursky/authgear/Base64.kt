package com.oursky.authgear

import android.util.Base64

internal fun base64UrlEncode(bytes: ByteArray): String {
    val flags = Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
    return Base64.encodeToString(bytes, flags)
}

internal fun bae64UrlDecode(str: String): ByteArray {
    val flags = Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
    return Base64.decode(str, flags)
}