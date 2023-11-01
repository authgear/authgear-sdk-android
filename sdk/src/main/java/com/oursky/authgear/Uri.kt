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
    // To my understanding, this is the safest way to rewrite origin.
    // Previously, we used the URI constructor, but we encountered a issue about
    // encoded query.
    // This affects us because we have x_state which is encoded a query.
    return this.buildUpon().scheme(newOrigin.scheme).encodedAuthority(newOrigin.encodedAuthority).build()
}
