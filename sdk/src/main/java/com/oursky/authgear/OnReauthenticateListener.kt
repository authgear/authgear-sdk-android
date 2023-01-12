package com.oursky.authgear

interface OnReauthenticateListener {
    fun onFinished(userInfo: UserInfo)
    fun onFailed(throwable: Throwable)
}
