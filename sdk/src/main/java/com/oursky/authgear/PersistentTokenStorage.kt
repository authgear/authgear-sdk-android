package com.oursky.authgear

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PersistentTokenStorage(val context: Context) : TokenStorage {
    companion object {
        private const val RefreshToken = "refreshToken"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

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
        return EncryptedSharedPreferences.create(
            context, namespace, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}