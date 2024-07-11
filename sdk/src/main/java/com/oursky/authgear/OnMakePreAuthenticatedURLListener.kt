package com.oursky.authgear

import android.net.Uri

interface OnMakePreAuthenticatedURLListener {
    fun onSuccess(uri: Uri)
    fun onFailed(throwable: Throwable)
}