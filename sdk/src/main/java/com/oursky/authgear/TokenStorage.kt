package com.oursky.authgear

interface TokenStorage {
    fun setRefreshToken(namespace: String, refreshToken: String)
    fun getRefreshToken(namespace: String): String?
    fun deleteRefreshToken(namespace: String)

    fun setIDToken(namespace: String, idToken: String)
    fun getIDToken(namespace: String): String?
    fun deleteIDToken(namespace: String)

    fun setDeviceSecret(namespace: String, deviceSecret: String)
    fun getDeviceSecret(namespace: String): String?
    fun deleteDeviceSecret(namespace: String)
}