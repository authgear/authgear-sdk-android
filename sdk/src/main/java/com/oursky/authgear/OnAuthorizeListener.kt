package com.oursky.authgear

interface OnAuthorizeListener {
    fun onAuthorized(state: String?)

    fun onAuthorizationFailed(throwable: Throwable)
}