package com.oursky.authgear

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal class PersistentContainerStorage(val context: Context) : ContainerStorage {
    companion object {
        const val Verifier = "verifier"
        const val AnonymousKeyId = "anonymousKeyId"
        const val BiometricKeyId = "biometricKeyId"
        const val App2AppDeviceKeyId = "app2appDeviceKeyId"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    override fun setOidcCodeVerifier(namespace: String, verifier: String) {
        getPref(namespace).edit()
            .putString(Verifier, verifier)
            .commit()
    }

    override fun getOidcCodeVerifier(namespace: String): String? {
        return getPref(namespace).getString(Verifier, null)
    }

    override fun getAnonymousKeyId(namespace: String): String? {
        return getPref(namespace).getString(AnonymousKeyId, null)
    }

    override fun setAnonymousKeyId(namespace: String, keyId: String) {
        getPref(namespace).edit()
            .putString(AnonymousKeyId, keyId)
            .commit()
    }

    override fun deleteAnonymousKeyId(namespace: String) {
        getPref(namespace).edit()
            .remove(AnonymousKeyId)
            .commit()
    }

    override fun getBiometricKeyId(namespace: String): String? {
        return getPref(namespace).getString(BiometricKeyId, null)
    }

    override fun setBiometricKeyId(namespace: String, keyId: String) {
        getPref(namespace).edit().putString(BiometricKeyId, keyId).commit()
    }

    override fun deleteBiometricKeyId(namespace: String) {
        getPref(namespace).edit().remove(BiometricKeyId).commit()
    }

    override fun getApp2AppDeviceKeyId(namespace: String): String? {
        return getPref(namespace).getString(App2AppDeviceKeyId, null)
    }
    override fun setApp2AppDeviceKeyId(namespace: String, keyId: String) {
        getPref(namespace).edit().putString(App2AppDeviceKeyId, keyId).commit()
    }
    override fun deleteApp2AppDeviceKeyId(namespace: String) {
        getPref(namespace).edit().remove(App2AppDeviceKeyId).commit()
    }

    private fun getPref(namespace: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context, namespace, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}