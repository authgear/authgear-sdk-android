package com.oursky.authgear

interface OnPromoteAnonymousUserListener {
    fun onPromoted(userInfo: UserInfo)
    fun onPromotionFailed(throwable: Throwable)
}
