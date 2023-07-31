package com.oursky.authgear.app2app

import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.ContainerStorage
import com.oursky.authgear.JWTHeader
import com.oursky.authgear.JWTHeaderType
import com.oursky.authgear.JWTPayload
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.data.oauth.OAuthRepo
import com.oursky.authgear.publicKeyToJWK
import com.oursky.authgear.signJWT
import java.security.KeyPair
import java.security.PrivateKey
import java.security.Signature
import java.time.Instant
import java.util.*

internal class App2App(
    private val namespace: String,
    private val storage: ContainerStorage,
    private val oauthRepo: OAuthRepo,
    private val keyRepo: KeyRepo
) {
    companion object {
        internal fun makeSignature(privateKey: PrivateKey): Signature {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            return signature
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun generateApp2AppJWT(forceNewKey: Boolean): String {
        val challenge = oauthRepo.oauthChallenge("app2app_request").token
        val existingKID: String? = storage.getApp2AppDeviceKeyId(namespace)
        val kid: String = if (existingKID == null || forceNewKey) {
            UUID.randomUUID().toString()
        } else {
            existingKID
        }
        val existingKeyPair: KeyPair? = keyRepo.getApp2AppDeviceKey(kid)
        val keyPair: KeyPair = if (existingKeyPair == null || forceNewKey) {
            keyRepo.generateApp2AppDeviceKey(kid)
        } else {
            existingKeyPair
        }

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
        val jwt = signJWT(signature, header, payload)

        storage.setApp2AppDeviceKeyId(namespace, kid)
        return jwt
    }
}