package com.oursky.authgear.dpop

import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.InterAppSharedStorage
import com.oursky.authgear.JWTHeader
import com.oursky.authgear.JWTHeaderType
import com.oursky.authgear.JWTPayload
import com.oursky.authgear.app2app.App2App
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.publicKeyToJWK
import com.oursky.authgear.signJWT
import com.oursky.authgear.toSHA256Thumbprint
import java.security.KeyPair
import java.time.Instant
import java.util.UUID

internal class DefaultDPoPProvider(
    private val namespace: String,
    private val sharedStorage: InterAppSharedStorage,
    private val keyRepo: KeyRepo
) : DPoPProvider {

    override fun generateDPoPProof(htm: String, htu: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null
        }
        val (kid, keypair) = getOrCreateDPoPPrivateKey()
        val jwk = publicKeyToJWK(kid, keypair.public)
        val header = JWTHeader(
            typ = JWTHeaderType.DPOPJWT,
            kid = kid,
            alg = jwk.alg,
            jwk = jwk
        )
        val payload = JWTPayload(
            now = Instant.now(),
            jti = UUID.randomUUID().toString(),
            htm = htm,
            htu = htu
        )
        val signature = App2App.makeSignature(keypair.private)
        return signJWT(signature, header, payload)
    }

    override fun computeJKT(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null
        }
        val (kid, keypair) = getOrCreateDPoPPrivateKey()
        val jwk = publicKeyToJWK(kid, keypair.public)
        return jwk.toSHA256Thumbprint()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getOrCreateDPoPPrivateKey(): Pair<String, KeyPair> {
        val existingKeyId = sharedStorage.getDPoPKeyId(namespace)
        existingKeyId?.let {kid ->
            val existingKey = keyRepo.getDPoPKey(kid)
            if (existingKey != null) {
                return Pair(kid, existingKey)
            }
        }
        val newKeyId = UUID.randomUUID().toString()
        val newKey = keyRepo.generateDPoPKey(newKeyId)
        sharedStorage.setDPoPKeyId(namespace, newKeyId)
        return Pair(newKeyId, newKey)
    }
}