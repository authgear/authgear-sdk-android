package com.oursky.authgear

class TransientTokenStorage : TokenStorage {
    private var refreshToken: MutableMap<String, String> = mutableMapOf()

    override fun setRefreshToken(namespace: String, refreshToken: String) {
        this.refreshToken[namespace] = refreshToken
    }

    override fun getRefreshToken(namespace: String): String? {
        return this.refreshToken[namespace]
    }

    override fun deleteRefreshToken(namespace: String) {
        this.refreshToken.remove(namespace)
    }
}