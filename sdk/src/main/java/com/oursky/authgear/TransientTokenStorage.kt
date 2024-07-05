package com.oursky.authgear

class TransientTokenStorage : TokenStorage {
    private var refreshToken: MutableMap<String, String> = mutableMapOf()
    private var idToken: MutableMap<String, String> = mutableMapOf()
    private var deviceSecret: MutableMap<String, String> = mutableMapOf()

    override fun setRefreshToken(namespace: String, refreshToken: String) {
        this.refreshToken[namespace] = refreshToken
    }

    override fun getRefreshToken(namespace: String): String? {
        return this.refreshToken[namespace]
    }

    override fun deleteRefreshToken(namespace: String) {
        this.refreshToken.remove(namespace)
    }

    override fun setIDToken(namespace: String, idToken: String) {
        this.idToken[namespace] = idToken
    }

    override fun getIDToken(namespace: String): String? {
        return this.idToken[namespace]
    }

    override fun deleteIDToken(namespace: String) {
        this.idToken.remove(namespace)
    }

    override fun setDeviceSecret(namespace: String, deviceSecret: String) {
        this.deviceSecret[namespace] = deviceSecret
    }

    override fun getDeviceSecret(namespace: String): String? {
        return this.deviceSecret[namespace]
    }

    override fun deleteDeviceSecret(namespace: String) {
        this.deviceSecret.remove(namespace)
    }
}