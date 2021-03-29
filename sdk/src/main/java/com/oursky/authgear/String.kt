package com.oursky.authgear

import java.nio.charset.StandardCharsets

internal fun String.toUTF8(): ByteArray {
    return this.toByteArray(StandardCharsets.UTF_8)
}