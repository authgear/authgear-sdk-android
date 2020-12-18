package com.oursky.authgear

interface OnOpenURLListener {
    fun onOpened()
    fun onFailed(throwable: Throwable)
}
