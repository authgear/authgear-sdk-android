package com.oursky.authgear

interface OnOpenURLListener {
    fun onClosed()
    fun onFailed(throwable: Throwable)
}
