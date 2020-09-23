package com.oursky.authgear

interface OnFetchUserInfoListener {
    fun onFetchedUserInfo(userInfo: UserInfo)
    fun onFetchingUserInfoFailed(throwable: Throwable)
}
