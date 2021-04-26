package com.oursky.authgear

interface OnRefreshAccessTokenIfNeededListener {
    fun onFinished()
    fun onFailed(throwable: Throwable)
}
