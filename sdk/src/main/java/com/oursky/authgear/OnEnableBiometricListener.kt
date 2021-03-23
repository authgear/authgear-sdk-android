package com.oursky.authgear

interface OnEnableBiometricListener {
    fun onEnabled()
    fun onFailed(throwable: Throwable)
}