package com.oursky.authgear

interface OnWeChatAuthCallbackListener {
    fun onWeChatAuthCallback()
    fun onWeChatAuthCallbackFailed(throwable: Throwable)
}
