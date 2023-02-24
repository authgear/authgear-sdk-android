package com.oursky.authgear.latte

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.oursky.authgear.getOrigin
import com.oursky.authgear.getQueryList
import com.oursky.authgear.rewriteOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

object LatteLink {
    const val BROADCAST_ACTION_LINK_RECEIVED = "com.oursky.authgear.latte.linkReceived"
    const val KEY_URI = "com.oursky.authgear.latte.linkReceived.uri"
    interface LinkHandler {
        suspend fun handle(latte: Latte)
    }

    private class ResetLinkHandler(
        private val query: List<Pair<String, String>>
    ) : LinkHandler {

        override suspend fun handle(latte: Latte) {
            val handle = latte.resetPassword(query)
            handle.finish()
        }
    }

    private class LoginLinkHandler(
        private val url: URL
    ) : LinkHandler {

        override suspend fun handle(latte: Latte) {
            withContext(Dispatchers.IO) {
                var httpConn: HttpURLConnection? = null
                var status: Int? = null
                try {
                    val conn = url.openConnection()
                    httpConn = conn as? HttpURLConnection ?: return@withContext
                    httpConn.requestMethod = "POST"
                    status = httpConn.responseCode
                } catch (_: IOException) {
                } finally {
                    httpConn?.disconnect()
                }
                if (status == null || status >= 400) {
                    // TODO(tung): Handle failure?
                    return@withContext
                }
            }
        }
    }

    fun getAppLinkHandler(
        intent: Intent,
        appLinkOrigin: Uri,
        rewriteAppLinkOrigin: Uri?
    ): LinkHandler? {
        var uri: Uri? = null
        val extraUriStr = intent.getStringExtra(KEY_URI)
        uri = if (extraUriStr != null) {
            Uri.parse(extraUriStr)
        } else {
            intent.data
        }
        if (uri == null) { return null }
        val origin = uri.getOrigin() ?: return null
        val path = uri.path ?: return null
        if (origin != appLinkOrigin.getOrigin()) { return null }
        val query = uri.getQueryList()
        return when {
            path.endsWith("/reset_link") -> ResetLinkHandler(query)
            path.endsWith("/login_link") -> {
                try {
                    if (rewriteAppLinkOrigin != null) {
                        uri = uri.rewriteOrigin(rewriteAppLinkOrigin)
                    }
                    val url = URL(uri.toString())
                    return LoginLinkHandler(url)
                } catch (e: MalformedURLException) { return null }
            }
            else -> null
        }
    }

    fun createLinkReceivedIntent(uri: Uri, app: Application): Intent {
        val intent = Intent(BROADCAST_ACTION_LINK_RECEIVED).apply {
            setPackage(app.packageName)
            putExtra(KEY_URI, uri.toString())
        }
        return intent
    }
}
