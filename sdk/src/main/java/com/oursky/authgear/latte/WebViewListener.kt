package com.oursky.authgear.latte

internal interface WebViewListener {
    @JvmDefault
    fun onEvent(event: WebViewEvent) {}

    @JvmDefault
    fun onReady(webView: WebView) {}

    @JvmDefault
    fun onComplete(webView: WebView, result: Result<WebViewResult>) {}
}