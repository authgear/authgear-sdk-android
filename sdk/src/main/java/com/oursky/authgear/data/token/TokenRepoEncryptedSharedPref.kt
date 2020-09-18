package com.oursky.authgear.data.token

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// This repository is synchronous so it's ok to use commit instead of apply.
@SuppressLint("ApplySharedPref")
internal class TokenRepoEncryptedSharedPref(private val applicationContext: Context) : TokenRepo {
    companion object {
        const val Verifier = "verifier"
        const val RefreshToken = "refreshToken"
    }
    private val masterKey = MasterKey.Builder(applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    override fun setOIDCCodeVerifier(namespace: String, verifier: String) {
        getPref(namespace).edit()
            .putString(Verifier, verifier)
            .commit()
    }
    override fun getOIDCCodeVerifier(namespace: String): String? {
        return getPref(namespace).getString(Verifier, null)
    }
    override fun setRefreshToken(namespace: String, refreshToken: String) {
        getPref(namespace).edit()
            .putString(RefreshToken, refreshToken)
            .commit()
    }
    override fun getRefreshToken(namespace: String): String? {
        return getPref(namespace).getString(RefreshToken, null)
    }
    override fun deleteRefreshToken(namespace: String) {
        getPref(namespace).edit()
            .remove(RefreshToken)
            .commit()
    }
    private fun getPref(namespace: String): SharedPreferences {
        return EncryptedSharedPreferences.create(applicationContext, namespace, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }
}
