package com.oursky.authgear

interface OnLogoutListener {
    fun onLogout()
    fun onLogoutFailed(throwable: Throwable)
}
