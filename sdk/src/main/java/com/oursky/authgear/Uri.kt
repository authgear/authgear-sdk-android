package com.oursky.authgear

import android.net.Uri
import java.net.URI
import java.net.URISyntaxException

internal fun Uri.getQueryList(): List<Pair<String, String>> {
    val keys = this.queryParameterNames
    val queryList = mutableListOf<Pair<String, String>>()
    for (key in keys) {
        val values = this.getQueryParameters(key)
        for (value in values) {
            queryList.add(Pair(key, value))
        }
    }
    return queryList
}

internal fun Uri.getOrigin(): String? {
    val thisUri = URI(this.toString())
    return try {
        URI(
            thisUri.scheme,
            null,
            thisUri.host,
            thisUri.port,
            null,
            null,
            null
        ).toString()
    } catch (e: URISyntaxException) {
        null
    }
}

internal fun Uri.rewriteOrigin(newOrigin: Uri): Uri {
    val originalUri = URI(this.toString())
    val newOriginUri = URI(newOrigin.toString())
    val newUri = URI(
        newOriginUri.scheme,
        originalUri.userInfo,
        newOriginUri.host,
        newOriginUri.port,
        originalUri.path,
        originalUri.query,
        originalUri.fragment
    )
    return Uri.parse(newUri.toString())
}