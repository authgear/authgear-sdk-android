package com.oursky.authgear.latte

internal sealed class WebViewEvent {
    object OpenEmailClient : WebViewEvent()
    data class ViewPage(val event: LatteViewPageEvent) : WebViewEvent()
}
