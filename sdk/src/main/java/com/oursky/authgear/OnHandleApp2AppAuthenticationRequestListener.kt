package com.oursky.authgear

interface OnHandleApp2AppAuthenticationRequestListener {
    fun onFinished()
    fun onFailed(throwable: Throwable)
}
