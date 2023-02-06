package com.oursky.authgear

interface OnVerifyEmailListener {
    fun onFinished()
    fun onFailed(throwable: Throwable)
}
