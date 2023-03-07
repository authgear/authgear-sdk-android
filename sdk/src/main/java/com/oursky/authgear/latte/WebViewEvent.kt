package com.oursky.authgear.latte

internal sealed class WebViewEvent {
    object OpenEmailClient : WebViewEvent()
    data class Analytics(val event: LatteAnalyticsEvent) : WebViewEvent()
}
