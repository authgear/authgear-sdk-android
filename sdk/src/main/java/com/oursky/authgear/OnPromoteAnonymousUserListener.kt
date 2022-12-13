package com.oursky.authgear

interface OnPromoteAnonymousUserListener {
    fun onPromoted(result: AuthenticateResult)
    fun onPromotionFailed(throwable: Throwable)
}
