package com.oursky.authgear.net

import java.lang.StringBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun Map<String, String>.toQueryParameter(): String {
    val queryBuilder = StringBuilder()
    val charset = StandardCharsets.UTF_8.toString()
    forEach { (key, value) ->
        queryBuilder.append("$key=${URLEncoder.encode(value, charset)}&")
    }
    return queryBuilder.toString()
}
