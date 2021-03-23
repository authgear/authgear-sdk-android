package com.oursky.authgear.data.token

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// This repository is synchronous so it's ok to use commit instead of apply.
// Note that encrypted shared preference not thread safe and we do not know whether editing the same shared pref is
// thread-safe so while per-field lock is more desirable this is not feasible for now.
@SuppressLint("ApplySharedPref")
internal class TokenRepoEncryptedSharedPref(private val applicationContext: Context) : TokenRepo {
    companion object {
        const val Verifier = "verifier"
        const val RefreshToken = "refreshToken"
        const val AnonymousKeyId = "anonymousKeyId"
        const val BiometricKeyId = "biometricKeyId"
    }

    private val masterKey = MasterKey.Builder(applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    override fun setOIDCCodeVerifier(namespace: String, verifier: String) {
        synchronized(this) {
            getPref(namespace).edit()
                .putString(Verifier, verifier)
                .commit()
        }
    }

    override fun getOIDCCodeVerifier(namespace: String): String? {
        synchronized(this) {
            return getPref(namespace).getString(Verifier, null)
        }
    }

    override fun setRefreshToken(namespace: String, refreshToken: String) {
        synchronized(this) {
            getPref(namespace).edit()
                .putString(RefreshToken, refreshToken)
                .commit()
        }
    }

    override fun getRefreshToken(namespace: String): String? {
        synchronized(this) {
            return getPref(namespace).getString(RefreshToken, null)
        }
    }

    override fun deleteRefreshToken(namespace: String) {
        synchronized(this) {
            getPref(namespace).edit()
                .remove(RefreshToken)
                .commit()
        }
    }

    override fun getAnonymousKeyId(namespace: String): String? {
        synchronized(this) {
            return getPref(namespace).getString(AnonymousKeyId, null)
        }
    }

    override fun setAnonymousKeyId(namespace: String, keyId: String) {
        synchronized(this) {
            getPref(namespace).edit()
                .putString(AnonymousKeyId, keyId)
                .commit()
        }
    }

    override fun deleteAnonymousKeyId(namespace: String) {
        synchronized(this) {
            getPref(namespace).edit()
                .remove(AnonymousKeyId)
                .commit()
        }
    }

    override fun getBiometricKeyId(namespace: String): String? {
        synchronized(this) {
            return getPref(namespace).getString(BiometricKeyId, null)
        }
    }

    override fun setBiometricKeyId(namespace: String, keyId: String) {
        synchronized(this) {
            getPref(namespace).edit().putString(BiometricKeyId, keyId).commit()
        }
    }

    override fun deleteBiometricKeyId(namespace: String) {
        synchronized(this) {
            getPref(namespace).edit().remove(BiometricKeyId).commit()
        }
    }

    private fun getPref(namespace: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            applicationContext, namespace, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
