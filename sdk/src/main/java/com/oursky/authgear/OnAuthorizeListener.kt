package com.oursky.authgear

interface OnAuthorizeListener {
    fun onAuthorized(result: AuthorizeResult)

    fun onAuthorizationFailed(throwable: Throwable)
}