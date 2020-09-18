package com.oursky.authgear.data.token

internal class TokenRepoInMemory : TokenRepo {
    override fun setOIDCCodeVerifier(namespace: String, verifier: String) {
    }
    override fun getOIDCCodeVerifier(namespace: String): String? {
        return null
    }
    override fun setRefreshToken(namespace: String, refreshToken: String) {
    }
    override fun getRefreshToken(namespace: String): String? {
        return null
    }
    override fun deleteRefreshToken(namespace: String) {
    }
}
