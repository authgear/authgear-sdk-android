package com.oursky.authgear

interface OnAuthenticateWithMigratedSessionListener {
    fun onAuthenticated(userInfo: UserInfo)
    fun onAuthenticationFailed(throwable: Throwable)
}
