package com.oursky.authgear

internal interface InterAppSharedStorage {
    fun setIDToken(namespace: String, idToken: String)
    fun getIDToken(namespace: String): String?
    fun deleteIDToken(namespace: String)

    fun setDeviceSecret(namespace: String, deviceSecret: String)
    fun getDeviceSecret(namespace: String): String?
    fun deleteDeviceSecret(namespace: String)

    fun setDPoPKeyId(namespace: String, keyId: String)
    fun getDPoPKeyId(namespace: String): String?
    fun deleteDPoPKeyId(namespace: String)

    fun onLogout(namespace: String)
}
