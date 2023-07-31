package com.oursky.authgear.app2app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.GrantType
import com.oursky.authgear.ServerException
import com.oursky.authgear.OAuthException
import com.oursky.authgear.AuthgearCore
import com.oursky.authgear.ContainerStorage
import com.oursky.authgear.JWTHeader
import com.oursky.authgear.JWTHeaderType
import com.oursky.authgear.JWTPayload
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.data.oauth.OAuthRepo
import com.oursky.authgear.net.toQueryParameter
import com.oursky.authgear.oauth.OidcTokenRequest
import com.oursky.authgear.publicKeyToJWK
import com.oursky.authgear.signJWT
import java.security.KeyPair
import java.security.PrivateKey
import java.security.Signature
import java.time.Instant
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class App2App(
    private val application: Application,
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

    fun createAuthenticateRequest(
        clientID: String,
        options: App2AppAuthenticateOptions,
        verifier: AuthgearCore.Verifier
    ): App2AppAuthenticateRequest {
        return options.toRequest(
            clientID = clientID,
            codeVerifier = verifier
        )
    }

    suspend fun startAuthenticateRequest(request: App2AppAuthenticateRequest): Uri {
        return suspendCoroutine { k ->
            var isResumed = false
            val uri = request.toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val intentFilter = IntentFilter(App2AppRedirectActivity.BROADCAST_ACTION)
            val br = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val resultUri =
                        intent?.getStringExtra(App2AppRedirectActivity.KEY_REDIRECT_URL) ?: return
                    if (isResumed) {
                        return
                    }
                    isResumed = true
                    application.unregisterReceiver(this)
                    k.resume(Uri.parse(resultUri))
                }
            }
            application.registerReceiver(br, intentFilter)
            application.startActivity(intent)
        }
    }

    fun parseApp2AppAuthenticationRequest(uri: Uri): App2AppAuthenticateRequest? {
        return App2AppAuthenticateRequest.parse(uri)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun doHandleApp2AppAuthenticationRequest(
        maybeRefreshToken: String?,
        redirectURI: Uri,
        request: App2AppAuthenticateRequest
    ): Intent {
        val refreshToken = maybeRefreshToken ?: throw OAuthException(
            error = "invalid_grant",
            errorDescription = "unauthenticated",
            state = null,
            errorURI = null
        )
        val jwt = generateApp2AppJWT(forceNewKey = false)
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OidcTokenRequest(
                grantType = GrantType.APP2APP,
                clientId = request.clientID,
                jwt = jwt,
                codeChallenge = request.codeChallenge,
                redirectUri = request.redirectUri,
                refreshToken = refreshToken,
                codeChallengeMethod = AuthgearCore.CODE_CHALLENGE_METHOD
            )
        )
        val query: Map<String, String> = hashMapOf(
            "code" to tokenResponse.code!!
        )
        val resultURI = redirectURI.buildUpon()
            .encodedQuery(query.toQueryParameter())
            .build()
        return Intent(Intent.ACTION_VIEW, resultURI)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun handleApp2AppAuthenticationRequest(maybeRefreshToken: String?, request: App2AppAuthenticateRequest) {
        var redirectURI: Uri? = null
        try {
            redirectURI = Uri.parse(request.redirectUri)
            val intent = doHandleApp2AppAuthenticationRequest(
                maybeRefreshToken,
                redirectURI,
                request
            )
            application.startActivity(intent)
        } catch (e: Throwable) {
            if (redirectURI == null) {
                // Can't parse redirect_uri, throw the error
                throw e
            }
            var error: String = "unknown_error"
            var errorDescription: String? = "Unknown error"
            when (e) {
                is OAuthException -> {
                    error = e.error
                    errorDescription = e.errorDescription
                }
                is ServerException -> {
                    error = "server_error"
                    errorDescription = e.message
                }
            }
            val query = hashMapOf(
                "error" to error
            )
            if (errorDescription != null) {
                query["error_description"] = errorDescription
            }
            val errorURI = redirectURI.buildUpon()
                .encodedQuery(query.toQueryParameter())
                .build()
            application.startActivity(Intent(Intent.ACTION_VIEW, errorURI))
        }
    }
    }