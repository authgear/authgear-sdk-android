package com.oursky.authgear

interface OnAuthenticateBiometricListener {
    fun onAuthenticated(userInfo: UserInfo)
    fun onAuthenticationFailed(throwable: Throwable)
}