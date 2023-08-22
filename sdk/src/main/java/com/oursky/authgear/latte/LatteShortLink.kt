package com.oursky.authgear.latte

import android.net.Uri
import com.oursky.authgear.data.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.StandardCharsets

class LatteShortLink(val uri: Uri, val appLinkOrigin: Uri, val rewriteAppLinkOrigin: Uri?) {
    suspend fun expand(): LatteAppLink? {
        return withContext(Dispatchers.IO) {
            val url = URL(uri.toString())
            HttpClient.fetch(
                url = url,
                method = "GET",
                headers = emptyMap(),
                followRedirect = false
            ) { conn ->
                conn.errorStream?.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                }
                if (conn.responseCode != 302) {
                    throw LatteException.InvalidShortLink
                }
                val locationList = conn.headerFields["Location"] ?: throw LatteException.InvalidShortLink
                if (locationList.isEmpty()) {
                    throw LatteException.InvalidShortLink
                }
                val location = locationList[0]
                val uri = Uri.parse(location)
                LatteAppLink.create(uri, appLinkOrigin, rewriteAppLinkOrigin)
            }
        }
    }
}