package com.oursky.authgear.latte

internal sealed class WebViewEvent {
    object OpenEmailClient : WebViewEvent()
    object OpenSMSClient : WebViewEvent()
    object ReauthWithBiometric : WebViewEvent()
    data class Tracking(val event: LatteTrackingEvent) : WebViewEvent()
}
