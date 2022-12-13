package com.oursky.authgear

internal interface ContainerStorage {
    fun setOidcCodeVerifier(namespace: String, verifier: String)
    fun getOidcCodeVerifier(namespace: String): String?

    fun getAnonymousKeyId(namespace: String): String?
    fun setAnonymousKeyId(namespace: String, keyId: String)
    fun deleteAnonymousKeyId(namespace: String)

    fun getBiometricKeyId(namespace: String): String?
    fun setBiometricKeyId(namespace: String, keyId: String)
    fun deleteBiometricKeyId(namespace: String)
}