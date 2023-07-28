package com.oursky.authgear.app2app

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.oursky.authgear.JWTHeader
import com.oursky.authgear.JWTHeaderType
import com.oursky.authgear.JWTPayload
import com.oursky.authgear.createKeyPair
import com.oursky.authgear.data.oauth.OAuthRepo
import com.oursky.authgear.publicKeyToJWK
import com.oursky.authgear.signJWT
import java.security.PrivateKey
import java.security.Signature
import java.time.Instant
import java.util.*

internal class App2App(
    private val oauthRepo: OAuthRepo
) {
    companion object {
        @RequiresApi(api = Build.VERSION_CODES.M)
        internal fun makeGenerateKeyPairSpec(alias: String): KeyGenParameterSpec {
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ) // API Level 23
                .setKeySize(2048) // API Level 23
                .setDigests(KeyProperties.DIGEST_SHA256) // API Level 23
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) // API Level 23
                .setUserAuthenticationRequired(false) // API Level 23

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(false)
            }
            return builder.build()
        }

        internal fun makeSignature(privateKey: PrivateKey): Signature {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            return signature
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun generateApp2AppJWT(): String {
        val challenge = oauthRepo.oauthChallenge("app2app_request").token
        val kid = UUID.randomUUID().toString()
        val alias = "com.authgear.keys.app2app.$kid"
        val spec = makeGenerateKeyPairSpec(alias)
        val keyPair = createKeyPair(spec)
        val jwk = publicKeyToJWK(kid, keyPair.public)
        val header = JWTHeader(
            typ = JWTHeaderType.APP2APP,
            kid = kid,
            alg = jwk.alg,
            jwk = jwk
        )
        val payload = JWTPayload(
            now = Instant.now(),
            challenge = challenge,
            action = "setup"
        )
        val signature = makeSignature(keyPair.private)
        return signJWT(signature, header, payload)
    }
}