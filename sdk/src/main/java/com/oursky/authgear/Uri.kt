package com.oursky.authgear

import android.net.Uri

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
    var originScheme = scheme ?: return null
    var originAuthority = host ?: return null
    if (port != -1) {
        originAuthority += ":$port"
    }
    return Uri.Builder()
        .scheme(originScheme)
        .authority(originAuthority).build().toString()
}