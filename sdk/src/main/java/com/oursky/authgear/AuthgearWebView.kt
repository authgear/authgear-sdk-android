package com.oursky.authgear

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient

internal class AuthgearWebView(
    context: Context
) : WebView(context) {
    init {
        setAuthgearWebViewClient(Client())
        settings.javaScriptEnabled = true
    }

    fun setAuthgearWebViewClient(client: Client) {
        super.setWebViewClient(client)
    }

    open class Client : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
        }
    }
}