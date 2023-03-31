package com.oursky.authgear.latte

internal sealed class WebViewEvent {
    object OpenEmailClient : WebViewEvent()
    data class Tracking(val event: LatteTrackingEvent) : WebViewEvent()
}
