package com.oursky.authgear.latte

import android.net.Uri
import com.oursky.authgear.AuthenticationRequest
import com.oursky.authgear.ExperimentalAuthgearApi

internal data class WebViewRequest(val url: Uri, val redirectUri: String) {
    @ExperimentalAuthgearApi
    constructor(request: AuthenticationRequest) : this(request.url, request.redirectUri) {
    }
}
