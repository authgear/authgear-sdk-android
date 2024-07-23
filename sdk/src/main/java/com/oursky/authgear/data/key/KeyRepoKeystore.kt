package com.oursky.authgear.data.key

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore

internal class KeyRepoKeystore : KeyRepo {

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun generateAnonymousKey(kid: String): KeyPair {
        val alias = "com.authgear.keys.anonymous.$kid"
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).build()
        )
        return kpg.generateKeyPair()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun getAnonymousKey(kid: String): KeyPair? {
        val alias = "com.authgear.keys.anonymous.$kid"

        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        val entry = ks.getEntry(alias, null)
        if (entry !is KeyStore.PrivateKeyEntry) {
            return null
        }

        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    private fun formatApp2AppDeviceKeyAlias(kid: String): String {
        return "com.authgear.keys.app2app.$kid"
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun generateApp2AppDeviceKey(kid: String): KeyPair {
        val alias = formatApp2AppDeviceKeyAlias(kid)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(false)
        }
        val spec = builder.build()
        val kpg =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun getApp2AppDeviceKey(kid: String): KeyPair? {
        val alias = formatApp2AppDeviceKeyAlias(kid)

        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        val entry = ks.getEntry(alias, null)
        if (entry !is KeyStore.PrivateKeyEntry) {
            return null
        }

        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun generateDPoPKey(kid: String): KeyPair {
        val alias = formatApp2AppDeviceKeyAlias(kid)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(false)
        }
        val spec = builder.build()
        val kpg =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun getDPoPKey(kid: String): KeyPair? {
        val alias = formatApp2AppDeviceKeyAlias(kid)

        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        val entry = ks.getEntry(alias, null)
        if (entry !is KeyStore.PrivateKeyEntry) {
            return null
        }

        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }
}
