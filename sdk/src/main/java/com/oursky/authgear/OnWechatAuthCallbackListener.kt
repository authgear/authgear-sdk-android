package com.oursky.authgear

interface OnWechatAuthCallbackListener {
    fun onWechatAuthCallback()
    fun onWechatAuthCallbackFailed(throwable: Throwable)
}
