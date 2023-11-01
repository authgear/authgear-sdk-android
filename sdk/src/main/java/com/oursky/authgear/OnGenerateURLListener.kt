package com.oursky.authgear

import android.net.Uri

@ExperimentalAuthgearApi
interface OnGenerateURLListener {
    fun onGenerated(uri: Uri)
    fun onFailed(throwable: Throwable)
}
