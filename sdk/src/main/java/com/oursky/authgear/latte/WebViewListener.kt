package com.oursky.authgear.latte

internal interface WebViewListener {
    fun onEvent(event: WebViewEvent) {}

    fun onReady(webView: WebView) {}

    fun onComplete(webView: WebView, result: Result<WebViewResult>) {}
}
