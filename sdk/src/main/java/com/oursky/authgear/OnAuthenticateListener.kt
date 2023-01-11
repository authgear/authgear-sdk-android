package com.oursky.authgear

interface OnAuthenticateListener {
    fun onAuthenticated(userInfo: UserInfo)
    fun onAuthenticationFailed(throwable: Throwable)
}
