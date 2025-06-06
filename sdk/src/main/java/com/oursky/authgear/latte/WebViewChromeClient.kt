package com.oursky.authgear.latte

import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE
import android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE

internal class WebViewChromeClient(private val webView: WebView) : android.webkit.WebChromeClient() {

    override fun onCreateWindow(
        view: android.webkit.WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (view == null) return false
        val result = view.hitTestResult
        return when (result.type) {
            SRC_IMAGE_ANCHOR_TYPE -> {
                // ref: https://pacheco.dev/posts/android/webview-image-anchor/
                val handler = view.handler
                val message = handler.obtainMessage()
                view.requestFocusNodeHref(message)
                val url = message.data.getString("url") ?: return false
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(browserIntent)
                return false
            }
            SRC_ANCHOR_TYPE -> {
                val data = result.extra ?: return false
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
                view.context.startActivity(browserIntent)
                return false
            }
            else -> false
        }
    }
}