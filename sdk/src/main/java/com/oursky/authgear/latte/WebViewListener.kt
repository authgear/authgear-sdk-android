package com.oursky.authgear.latte

internal interface WebViewListener {
    @JvmDefault
    fun onCompleted(result: WebViewResult) {}

    @JvmDefault
    fun onEvent(event: WebViewEvent) {}
}