package com.oursky.authgear.data.token

internal class TokenRepoInMemory(private var refreshTokenMap: MutableMap<String, String> = mutableMapOf<String, String>()) : TokenRepo {
    override fun setOIDCCodeVerifier(namespace: String, verifier: String) {
    }

    override fun getOIDCCodeVerifier(namespace: String): String? {
        return null
    }

    override fun setRefreshToken(namespace: String, refreshToken: String) {
        this.refreshTokenMap[namespace] = refreshToken
    }

    override fun getRefreshToken(namespace: String): String? {
        return this.refreshTokenMap[namespace]
    }

    override fun deleteRefreshToken(namespace: String) {
    }

    override fun getAnonymousKeyId(namespace: String): String? {
        return null
    }

    override fun setAnonymousKeyId(namespace: String, keyId: String) {
    }

    override fun deleteAnonymousKeyId(namespace: String) {
    }

    override fun getBiometricKeyId(namespace: String): String? {
        return null
    }

    override fun setBiometricKeyId(namespace: String, keyId: String) {
    }

    override fun deleteBiometricKeyId(namespace: String) {
    }
}
