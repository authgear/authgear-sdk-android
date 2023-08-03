package com.oursky.authgear.app2app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.*
import com.oursky.authgear.data.assetlink.AssetLink
import com.oursky.authgear.data.assetlink.AssetLinkRepo
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.data.oauth.OAuthRepo
import com.oursky.authgear.net.toQueryParameter
import com.oursky.authgear.oauth.OidcTokenRequest
import java.security.KeyPair
import java.security.MessageDigest
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
    private val keyRepo: KeyRepo,
    private val assetLinkRepo: AssetLinkRepo
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
        val uri = request.toUri()
        try {
            verifyAppIntegrityByUri(uri)
        } catch (integrityCheckError: Throwable) {
            throw OAuthException(
                error = "invalid_client",
                errorDescription = "integrity check error: ${integrityCheckError.message}",
                errorURI = null,
                state = null
            )
        }
        return suspendCoroutine { k ->
            var isResumed = false
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
        try {
            verifyAppIntegrityByUri(redirectURI)
        } catch (integrityCheckError: Throwable) {
            throw OAuthException(
                error = "invalid_client",
                errorDescription = "integrity check error: ${integrityCheckError.message}",
                errorURI = null,
                state = null
            )
        }
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)
        } catch (e: Throwable) {
            if (redirectURI == null) {
                // Can't parse redirect_uri, throw the error
                throw e
            }
            var error = "unknown_error"
            var errorDescription: String? = (e.message ?: "Unknown error")
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
            val intent = Intent(Intent.ACTION_VIEW, errorURI)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)
        }
    }

    private suspend fun verifyAppIntegrityByUri(uri: Uri) {
        val assetLinks: List<AssetLink> = assetLinkRepo.getAssetLinks(uri)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val pm = application.packageManager
        val packageInfos = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
        val packageNames = hashSetOf<String>()
        for (info in packageInfos) {
            packageNames.add(info.activityInfo.packageName)
        }
        var matchedPackageLink: AssetLink? = null
        for (name in packageNames) {
            val entry = assetLinks.find { al ->
                al.relation.contains("delegate_permission/common.handle_all_urls") &&
                    al.target.packageName == name
            }
            if (entry != null) {
                matchedPackageLink = entry
                break
            }
        }
        if (matchedPackageLink == null) {
            throw AuthgearException("No package found to handle uri: $uri")
        }
        val expectedPackageName = matchedPackageLink.target.packageName
        val actualSigningCertHashes = getSigningCertificatesHexHashes(expectedPackageName)
        val expectedHashes = matchedPackageLink.target.sha256CertFingerprints.map { fingerprints ->
            fingerprints.replace(":", "").toLowerCase(Locale.ROOT)
        }.toSet()
        if (
            actualSigningCertHashes != null &&
            actualSigningCertHashes.intersect(expectedHashes).isNotEmpty()
        ) {
            return
        }
        throw AuthgearException("package integrity cannot be verified: $expectedPackageName")
    }

    private fun getSigningCertificatesHexHashes(packageName: String): Set<String?>? {
        // NOTE(tung): This function references https://github.com/openid/AppAuth-Android/blob/300a91d24c2f085889cdd336a95031d71acd1257/library/java/net/openid/appauth/app2app/SecureRedirection.java#L246

        return try {
            val pm = application.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo: SigningInfo = pm
                    .getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    ).signingInfo
                signingInfo.signingCertificateHistory
            } else {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures
            }
            return generateSignatureHexHashes(signatures)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun generateSignatureHexHashes(
        signatures: Array<android.content.pm.Signature?>
    ): Set<String> {
        // NOTE(tung): This function reference https://github.com/openid/AppAuth-Android/blob/300a91d24c2f085889cdd336a95031d71acd1257/library/java/net/openid/appauth/browser/BrowserDescriptor.java#L157C1-L165C6
        val signatureHashes: MutableSet<String> = HashSet()
        for (signature in signatures) {
            val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
            val hashBytes: ByteArray = digest.digest(signature!!.toByteArray())
            signatureHashes.add(hashBytes.toHex())
        }
        return signatureHashes
    }
}

fun ByteArray.toHex(): String {
    val sb: StringBuilder = StringBuilder(this.size * 2)
    for (b in this) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString().toLowerCase(Locale.ROOT)
}