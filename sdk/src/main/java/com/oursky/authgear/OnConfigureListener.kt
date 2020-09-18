package com.oursky.authgear

interface OnConfigureListener {
    fun onConfigured()
    fun onConfigurationFailed(throwable: Throwable)
}
