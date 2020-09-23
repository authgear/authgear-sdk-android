package com.oursky.authgear

interface OnPromoteAnonymousUserListener {
    fun onPromoted(result: AuthorizeResult)
    fun onPromotionFailed(throwable: Throwable)
}
