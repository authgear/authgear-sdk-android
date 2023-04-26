package com.oursky.authgear.latte

import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.annotation.RequiresApi

class WebViewError(val errorCode: Int, val description: String, val url: String) : RuntimeException("$errorCode $url $description") {
    @RequiresApi(api = Build.VERSION_CODES.M)
    constructor(request: WebResourceRequest, error: WebResourceError) : this(error.errorCode, error.description.toString(), request.url.toString())
}