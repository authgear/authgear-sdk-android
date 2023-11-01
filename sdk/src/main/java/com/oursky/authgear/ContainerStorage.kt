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

    fun getApp2AppDeviceKeyId(namespace: String): String?
    fun setApp2AppDeviceKeyId(namespace: String, keyId: String)
    fun deleteApp2AppDeviceKeyId(namespace: String)
}