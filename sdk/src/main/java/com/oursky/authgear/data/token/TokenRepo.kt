package com.oursky.authgear.data.token

internal interface TokenRepo {
    fun setOIDCCodeVerifier(namespace: String, verifier: String)
    fun getOIDCCodeVerifier(namespace: String): String?

    fun getAnonymousKeyId(namespace: String): String?
    fun setAnonymousKeyId(namespace: String, keyId: String)
    fun deleteAnonymousKeyId(namespace: String)

    fun getBiometricKeyId(namespace: String): String?
    fun setBiometricKeyId(namespace: String, keyId: String)
    fun deleteBiometricKeyId(namespace: String)
}
