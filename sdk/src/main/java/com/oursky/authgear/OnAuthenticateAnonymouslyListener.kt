package com.oursky.authgear

interface OnAuthenticateAnonymouslyListener {
    fun onAuthenticated(userInfo: UserInfo)
    fun onAuthenticationFailed(throwable: Throwable)
}
