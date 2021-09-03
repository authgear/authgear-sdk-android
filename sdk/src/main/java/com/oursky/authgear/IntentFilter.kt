package com.oursky.authgear

import java.security.SecureRandom

internal fun newRandomAction(): String {
    val rng = SecureRandom()
    val byteArray = ByteArray(32)
    rng.nextBytes(byteArray)
    val action = base64UrlEncode(byteArray)
    return action
}