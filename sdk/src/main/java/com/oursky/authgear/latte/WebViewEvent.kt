package com.oursky.authgear.latte

internal sealed class WebViewEvent {
    object OpenEmailClient : WebViewEvent()
}

internal class ViewPageEvent(val path: String) : WebViewEvent()
