package com.oursky.authgear.data.token

internal interface TokenRepo {
    fun setRefreshToken(namespace: String, refreshToken: String)
    fun getRefreshToken(namespace: String): String?
    fun deleteRefreshToken(namespace: String)
    fun setOIDCCodeVerifier(namespace: String, verifier: String)
    fun getOIDCCodeVerifier(namespace: String): String?
}
