package com.oursky.authgear

interface OnOpenSettingsActionListener {
    fun onFinished()
    fun onFailed(throwable: Throwable)
}
