package com.oursky.authgear.latte

import android.annotation.SuppressLint
import android.content.Context
import com.oursky.authgear.AuthgearException

@SuppressLint("SetJavaScriptEnabled")
internal class WebView(context: Context) : android.webkit.WebView(context) {
    var request: WebViewRequest? = null
    var listener: WebViewListener? = null

    init {
        addJavascriptInterface(WebViewJSInterface(this), WebViewJSInterface.jsBridgeName)
        settings.javaScriptEnabled = true

        webViewClient = android.webkit.WebViewClient()
        // TODO: copy WebChromeClient from OAuthWebViewBaseActivity?
    }

    fun load() {
        val url = this.request?.url ?: throw AuthgearException("request not provided")
        this.loadUrl(url.toString())
    }

    override fun setWebViewClient(client: android.webkit.WebViewClient) {
        super.setWebViewClient(WebViewClient(this, client))
    }
}