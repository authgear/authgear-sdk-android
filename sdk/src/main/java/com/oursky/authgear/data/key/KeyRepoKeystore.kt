package com.oursky.authgear.data.key

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.*

class KeyRepoKeystore : KeyRepo {
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun getAnonymousKey(kid: String?, isUrlSafe: Boolean): JwkResponse {
        val resolvedKid = kid ?: UUID.randomUUID().toString()
        val alias = "com.authgear.keys.anonymous.$resolvedKid"
        val entry = loadKey(alias)
        var jwk: Jwk? = null
        if (entry == null) {
            val kp = generateKey(alias)
            val pubKey = kp.public as RSAPublicKey
            var base64Flags = Base64.NO_WRAP
            if (isUrlSafe) base64Flags = base64Flags or Base64.URL_SAFE or Base64.NO_PADDING
            jwk = Jwk(
                resolvedKid,
                "RSA",
                Base64.encodeToString(pubKey.modulus.toByteArray(), base64Flags),
                Base64.encodeToString(pubKey.publicExponent.toByteArray(), base64Flags)
            )
        }
        return JwkResponse(resolvedKid, "RS256", jwk)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun signAnonymousToken(kid: String, data: String, isUrlSafe: Boolean): String {
        val alias = "com.authgear.keys.anonymous.$kid"
        val entry = loadKey(alias) ?: throw IllegalArgumentException("Anonymous user key not found.")
        val s = Signature.getInstance("SHA256withRSA")
        s.initSign(entry.privateKey)
        s.update(data.toByteArray(StandardCharsets.UTF_8))
        val signature = s.sign()
        var base64Flags = Base64.NO_WRAP
        if (isUrlSafe) base64Flags = base64Flags or Base64.URL_SAFE or Base64.NO_PADDING
        return Base64.encodeToString(signature, base64Flags)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun loadKey(alias: String): KeyStore.PrivateKeyEntry? {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val entry = ks.getEntry(alias, null)
        if (entry !is KeyStore.PrivateKeyEntry) {
            return null
        }
        return entry
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun generateKey(alias: String): KeyPair {
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
}
