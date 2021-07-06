package com.oursky.authgear

interface OnRefreshIDTokenListener {
    fun onFinished()
    fun onFailed(throwable: Throwable)
}
