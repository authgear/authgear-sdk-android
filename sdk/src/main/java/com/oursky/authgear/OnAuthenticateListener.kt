package com.oursky.authgear

interface OnAuthenticateListener {
    fun onAuthenticated(result: AuthenticateResult)

    fun onAuthenticationFailed(throwable: Throwable)
}