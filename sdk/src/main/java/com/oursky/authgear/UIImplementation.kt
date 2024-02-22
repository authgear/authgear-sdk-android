package com.oursky.authgear

import android.content.Context
import android.net.Uri

class OpenAuthorizationURLOptions(val url: Uri, val redirectURI: Uri)

interface OnOpenAuthorizationURLListener {
    fun onSuccess(url: Uri)
    fun onFailed(throwable: Throwable)
}

interface UIImplementation {
    fun openAuthorizationURL(context: Context, options: OpenAuthorizationURLOptions, listener: OnOpenAuthorizationURLListener)
}
