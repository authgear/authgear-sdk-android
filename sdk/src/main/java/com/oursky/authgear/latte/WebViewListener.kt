package com.oursky.authgear.latte

internal interface WebViewListener {
    @JvmDefault
    fun onEvent(event: WebViewEvent) {}
}