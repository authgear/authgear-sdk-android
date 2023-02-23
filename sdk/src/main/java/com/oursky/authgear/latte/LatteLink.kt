package com.oursky.authgear.latte

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.oursky.authgear.getQueryList

object LatteLink {
    const val BROADCAST_ACTION_LINK_RECEIVED = "com.oursky.authgear.latte.linkReceived"
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

    fun getAppLinkHandler(
        linkURLHost: String,
        intent: Intent
    ): LinkHandler? {
        val uri = intent.data ?: return null
        val host = uri.host ?: return null
        val path = uri.path ?: return null
        if (host != linkURLHost) { return null }
        val query = uri.getQueryList()
        return when {
            path.endsWith("/reset_link") -> ResetLinkHandler(query)
            else -> null
        }
    }

    fun createLinkReceivedIntent(uri: Uri, app: Application): Intent {
        val intent = Intent(BROADCAST_ACTION_LINK_RECEIVED).apply {
            setPackage(app.packageName)
            data = uri
        }
        return intent
    }
}