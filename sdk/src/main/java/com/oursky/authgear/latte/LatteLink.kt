package com.oursky.authgear.latte

import android.content.Intent
import com.oursky.authgear.getQueryList

object LatteLink {
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
}