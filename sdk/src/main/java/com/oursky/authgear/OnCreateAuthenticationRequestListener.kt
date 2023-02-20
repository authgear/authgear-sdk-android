package com.oursky.authgear

@ExperimentalAuthgearApi
interface OnCreateAuthenticationRequestListener {
    fun onCreated(request: AuthenticationRequest)
    fun onFailed(throwable: Throwable)
}