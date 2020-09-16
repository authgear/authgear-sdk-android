package com.oursky.authgear.data.token

internal interface TokenRepo {
    fun setRefreshToken(namespace: String, refreshToken: String)
}
