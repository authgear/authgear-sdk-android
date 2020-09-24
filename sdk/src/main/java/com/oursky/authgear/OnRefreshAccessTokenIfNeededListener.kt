package com.oursky.authgear

interface OnRefreshAccessTokenIfNeededListener {
    fun onFinished(accessToken: String?)
    fun onFailed(throwable: Throwable)
}
