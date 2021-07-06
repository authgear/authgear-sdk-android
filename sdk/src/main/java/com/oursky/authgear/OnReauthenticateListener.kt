package com.oursky.authgear

interface OnReauthenticateListener {
    fun onFinished(result: ReauthenticateResult)
    fun onFailed(throwable: Throwable)
}