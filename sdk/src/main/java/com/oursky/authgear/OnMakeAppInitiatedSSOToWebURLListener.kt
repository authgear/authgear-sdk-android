package com.oursky.authgear

import android.net.Uri

interface OnMakeAppInitiatedSSOToWebURLListener {
    fun onSuccess(uri: Uri)
    fun onFailed(throwable: Throwable)
}