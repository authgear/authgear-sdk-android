package com.oursky.authgear

@ExperimentalAuthgearApi
interface OnAuthenticateWithMigratedSessionListener {
    fun onAuthenticated(userInfo: UserInfo)
    fun onAuthenticationFailed(throwable: Throwable)
}
