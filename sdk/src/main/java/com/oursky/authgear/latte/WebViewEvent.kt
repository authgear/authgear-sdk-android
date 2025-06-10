package com.oursky.authgear.latte

import android.net.Uri

internal sealed class WebViewEvent {
    object OpenEmailClient : WebViewEvent()
    object OpenSMSClient : WebViewEvent()
    object ReauthWithBiometric : WebViewEvent()
    object ResetPasswordCompleted : WebViewEvent()
    data class Tracking(val event: LatteTrackingEvent) : WebViewEvent()
    data class OpenExternalURL(val uri: Uri): WebViewEvent()
}
