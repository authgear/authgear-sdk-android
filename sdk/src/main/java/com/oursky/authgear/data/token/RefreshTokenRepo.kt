package com.oursky.authgear.data.token

internal interface RefreshTokenRepo {
    fun setRefreshToken(namespace: String, refreshToken: String)
    fun getRefreshToken(namespace: String): String?
    fun deleteRefreshToken(namespace: String)
}