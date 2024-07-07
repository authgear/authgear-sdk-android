package com.oursky.authgear

interface TokenStorage {
    fun setRefreshToken(namespace: String, refreshToken: String)
    fun getRefreshToken(namespace: String): String?
    fun deleteRefreshToken(namespace: String)
}