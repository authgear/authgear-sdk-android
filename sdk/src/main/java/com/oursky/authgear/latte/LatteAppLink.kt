package com.oursky.authgear.latte

import android.net.Uri
import com.oursky.authgear.data.HttpClient
import com.oursky.authgear.getOrigin
import com.oursky.authgear.rewriteOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.StandardCharsets

sealed class LatteAppLink {

    class ResetLink(val uri: Uri) : LatteAppLink()

    class LoginLink(val uri: Uri) : LatteAppLink() {
        suspend fun handle(latte: Latte) {
            return withContext(Dispatchers.IO) {
                val url = URL(uri.toString())
                HttpClient.fetch(url, "POST", emptyMap()) { conn ->
                    conn.errorStream?.use {
                        val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                        HttpClient.throwErrorIfNeeded(conn, responseString)
                    }
                    conn.inputStream.use {
                        val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                        HttpClient.throwErrorIfNeeded(conn, responseString)
                    }
                }
            }
        }
    }

    companion object {
        fun create(uri: Uri, appLinkOrigin: Uri, rewriteAppLinkOrigin: Uri?): LatteAppLink? {
            var linkUri = uri
            val origin = uri.getOrigin() ?: return null
            val path = uri.path ?: return null
            if (origin != appLinkOrigin.getOrigin()) {
                return null
            }
            if (rewriteAppLinkOrigin != null) {
                linkUri = uri.rewriteOrigin(rewriteAppLinkOrigin)
            }

            val link = when {
                path.endsWith("/reset_link") -> ResetLink(linkUri)
                path.endsWith("/login_link") -> LoginLink(linkUri)
                else -> null
            }

            return link
        }
    }
}
