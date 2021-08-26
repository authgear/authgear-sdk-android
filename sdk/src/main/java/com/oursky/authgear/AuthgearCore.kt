package com.oursky.authgear

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.data.oauth.OauthRepo
import com.oursky.authgear.data.token.TokenRepo
import com.oursky.authgear.data.token.TokenRepoInMemory
import com.oursky.authgear.net.toQueryParameter
import com.oursky.authgear.oauth.OIDCAuthenticationRequest
import com.oursky.authgear.oauth.OIDCTokenRequest
import com.oursky.authgear.oauth.OIDCTokenResponse
import com.oursky.authgear.oauth.toQuery
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Listeners should only be set on the main thread.
 */
internal class AuthgearCore(
    private val authgear: Authgear,
    private val application: Application,
    val clientId: String,
    private val authgearEndpoint: String,
    private val storageType: StorageType,
    private val shareSessionWithDeviceBrowser: Boolean,
    private val tokenRepo: TokenRepo,
    private val oauthRepo: OauthRepo,
    private val keyRepo: KeyRepo,
    name: String? = null
) {
    companion object {
        @Suppress("unused")
        private val TAG = AuthgearCore::class.java.simpleName

        /**
         * To prevent user from using expired access token, we have to check in advance
         * whether it had expired and refresh it accordingly in [refreshAccessTokenIfNeeded]. If we
         * use the expiry time in [OIDCTokenResponse] directly to check for expiry, it is possible
         * that the access token had passed the check but ends up being expired when it arrives at
         * the server due to slow traffic or unfair scheduler.
         *
         * To compat this, we should consider the access token expired earlier than the expiry time
         * calculated using [OIDCTokenResponse.expiresIn]. Current implementation uses
         * [ExpireInPercentage] of [OIDCTokenResponse.expiresIn] to calculate the expiry time.
         */
        private const val ExpireInPercentage = 0.9

        /*
         * GlobalMemoryStore is used when calling configure with transientSession
         * Using same global memory store to ensure the refresh token can be retained in the whole
         * app lifecycle
         */
        private var GlobalMemoryStore: TokenRepo = TokenRepoInMemory()
        /**
         * A map used to keep track of which deep link is being handled by which container.
         */
        private val DeepLinkHandlerMap = mutableMapOf<String, SuspendHolder<String>>()
        fun handleDeepLink(deepLink: String, isSuccessful: Boolean) {
            if (isSuccessful) {
                // The deep link would contain code in query parameter so we trim it to get back the handler.
                val deepLinkWithoutQuery = getURLWithoutQuery(deepLink)
                val handler = requireDeepLinkHandler(deepLinkWithoutQuery)
                handler.continuation.resume(deepLink)
                DeepLinkHandlerMap.remove(deepLinkWithoutQuery)
            } else {
                val handler = requireDeepLinkHandler(deepLink)
                handler.continuation.resumeWith(Result.failure(CancelException()))
                DeepLinkHandlerMap.remove(deepLink)
            }
        }

        private fun requireDeepLinkHandler(deepLink: String): SuspendHolder<String> {
            val handler = DeepLinkHandlerMap[deepLink]
            require(handler != null) {
                "No handler is handling deep link $deepLink"
            }
            return handler
        }

        /**
         * Check and handle wehchat redirect uri and trigger delegate function if needed
         */
        private var wechatRedirectURI: String? = null
        private var wechatRedirectHandler: WechatRedirectHandler? = null
        fun registerWechatRedirectURI(uri: String?, handler: WechatRedirectHandler) {
            if (uri != null) {
                wechatRedirectURI = uri
                wechatRedirectHandler = handler
            } else {
                unregisteredWechatRedirectURI()
            }
        }

        fun unregisteredWechatRedirectURI() {
            wechatRedirectURI = null
            wechatRedirectHandler = null
        }

        /**
         * handleWeChatRedirectDeepLink return true if it is handled
         */
        fun handleWechatRedirectDeepLink(deepLink: String): Boolean {
            if (wechatRedirectURI == null) {
                return false
            }
            val deepLinkWithoutQuery = getURLWithoutQuery(deepLink)
            if (deepLinkWithoutQuery != wechatRedirectURI) {
                return false
            }
            val uri = Uri.parse(deepLink)
            val state = uri.getQueryParameter("state")
            if (state != null) {
                wechatRedirectHandler?.sendWechatAuthRequest(state)
            }
            return true
        }

        private fun getURLWithoutQuery(input: String): String {
            val uri = Uri.parse(input)
            var builder = uri.buildUpon().clearQuery()
            builder = builder.fragment("")
            return builder.build().toString()
        }
    }

    interface WechatRedirectHandler {
        fun sendWechatAuthRequest(state: String)
    }

    data class SuspendHolder<T>(val name: String, val continuation: Continuation<T>)

    data class Verifier(
        val verifier: String,
        val challenge: String
    )

    private val name = name ?: "default"
    private var isInitialized = false
    private var refreshToken: String? = null
    var accessToken: String? = null
        private set
    var idToken: String? = null
        private set
    private var expireAt: Instant? = null
    var sessionState: SessionState = SessionState.UNKNOWN
        private set
    private val refreshAccessTokenJob = AtomicReference<Job>(null)
    var delegate: AuthgearDelegate? = null
    private val refreshTokenRepo: TokenRepo

    init {
        oauthRepo.endpoint = authgearEndpoint
        if (storageType == StorageType.TRANSIENT) {
            refreshTokenRepo = GlobalMemoryStore
        } else {
            refreshTokenRepo = tokenRepo
        }
    }

    val canReauthenticate: Boolean
        get() {
            val idToken = this.idToken
            if (idToken == null) {
                return false
            }
            val jsonObject = decodeJWT(idToken)
            val can = jsonObject["https://authgear.com/claims/user/can_reauthenticate"]
            return can?.jsonPrimitive?.booleanOrNull ?: false
        }

    val authTime: Date?
        get() {
            val idToken = this.idToken
            if (idToken == null) {
                return null
            }
            val jsonObject = decodeJWT(idToken)
            val authTimeValue = jsonObject["auth_time"]?.jsonPrimitive?.longOrNull
            if (authTimeValue == null) {
                return null
            }
            return Date(authTimeValue * 1000)
        }

    private fun requireIsInitialized() {
        require(isInitialized) {
            "Authgear is not configured. Did you forget to call configure?"
        }
    }

    private fun requireMinimumBiometricAPILevel() {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            "Biometric authentication requires at least API Level 23"
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun authenticateAnonymously(): UserInfo {
        requireIsInitialized()
        val challenge = oauthRepo.oauthChallenge("anonymous_request").token

        var keyId = tokenRepo.getAnonymousKeyId(name)
        var keyPair: KeyPair

        if (keyId == null) {
            keyId = UUID.randomUUID().toString()
            keyPair = keyRepo.generateAnonymousKey(keyId)
        } else {
            val maybeKeyPair = keyRepo.getAnonymousKey(keyId)
            if (maybeKeyPair == null) {
                throw AnonymousUserNotFoundException()
            }
            keyPair = maybeKeyPair
        }

        val jwk = publicKeyToJWK(keyId, keyPair.public)

        val header = JWTHeader(
            typ = JWTHeaderType.ANONYMOUS,
            kid = jwk.kid,
            alg = jwk.alg,
            jwk = jwk
        )
        val payload = JWTPayload(
            now = Instant.now(),
            challenge = challenge,
            action = "auth",
            deviceInfo = getDeviceInfo(this.application)
        )

        val signature = makeSignature(keyPair.private)
        val jwt = signJWT(signature, header, payload)

        val tokenResponse = oauthRepo.oidcTokenRequest(
            OIDCTokenRequest(
                grantType = GrantType.ANONYMOUS,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                jwt = jwt
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(
            tokenResponse.accessToken!!
        )
        saveToken(tokenResponse, SessionStateChangeReason.AUTHENTICATED)
        disableBiometric()
        tokenRepo.setAnonymousKeyId(name, keyId)
        return userInfo
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun authorize(options: AuthorizeOptions): AuthorizeResult {
        requireIsInitialized()
        val codeVerifier = this.setupVerifier()
        val request = options.toRequest(shouldSuppressIDPSessionCookie())
        val authorizeUrl = authorizeEndpoint(request, codeVerifier)
        val deepLink = openAuthorizeUrl(request.redirectUri, authorizeUrl)
        return finishAuthorization(deepLink)
    }

    suspend fun reauthenticate(
        options: ReauthentcateOptions,
        biometricOptions: BiometricOptions?
    ): ReauthenticateResult {
        requireIsInitialized()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val biometricEnabled = this.isBiometricEnabled()
            if (biometricEnabled && biometricOptions != null) {
                val userInfo = this.authenticateBiometric(biometricOptions)
                return ReauthenticateResult(userInfo = userInfo, state = options.state)
            }
        }

        if (!this.canReauthenticate) {
            throw AuthgearException("canReauthenticate is false")
        }
        val idTokenHint = this.idToken
        if (idTokenHint == null) {
            throw AuthgearException("Call refreshIDToken first")
        }
        val codeVerifier = this.setupVerifier()
        val request = options.toRequest(idTokenHint, shouldSuppressIDPSessionCookie())
        val authorizeUrl = authorizeEndpoint(request, codeVerifier)
        val deepLink = openAuthorizeUrl(request.redirectUri, authorizeUrl)
        return finishReauthentication(deepLink)
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun configure() {
        isInitialized = true
        val refreshToken = refreshTokenRepo.getRefreshToken(name)
        this.refreshToken = refreshToken
        if (refreshToken != null) {
            // Consider user as logged in if refresh token is available
            updateSessionState(SessionState.AUTHENTICATED, SessionStateChangeReason.FOUND_TOKEN)
        } else {
            updateSessionState(SessionState.NO_SESSION, SessionStateChangeReason.NO_TOKEN)
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun logout(force: Boolean? = null) {
        requireIsInitialized()
        try {
            val refreshToken = refreshTokenRepo.getRefreshToken(name) ?: ""
            oauthRepo.oidcRevocationRequest(refreshToken)
        } catch (e: Exception) {
            if (force != true) {
                throw e
            }
        }
        clearSession(SessionStateChangeReason.LOGOUT)
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun wechatAuthCallback(code: String, state: String) {
        requireIsInitialized()
        oauthRepo.wechatAuthCallback(code, state)
    }

    @MainThread
    fun openUrl(path: String, options: SettingOptions? = null) {
        requireIsInitialized()

        val refreshToken = refreshTokenRepo.getRefreshToken(name)
            ?: throw UnauthenticatedUserException()
        val token = oauthRepo.oauthAppSessionToken(refreshToken).appSessionToken

        val url = URL(URL(authgearEndpoint), path).toString()

        val loginHint = "https://authgear.com/login_hint?type=app_session_token&app_session_token=${
        URLEncoder.encode(token, StandardCharsets.UTF_8.name())
        }"
        val authorizeUrl = authorizeEndpoint(
            OIDCAuthenticationRequest(
                redirectUri = url.toString(),
                responseType = "none",
                scope = listOf("openid", "offline_access", "https://authgear.com/scopes/full-access"),
                prompt = listOf(PromptOption.NONE),
                loginHint = loginHint,
                wechatRedirectURI = options?.wechatRedirectURI,
                suppressIDPSessionCookie = shouldSuppressIDPSessionCookie()
            ),
            null
        )

        application.startActivity(
            WebViewActivity.createIntent(application, Uri.parse(authorizeUrl))
        )
    }

    fun open(page: Page, options: SettingOptions? = null) {
        openUrl(
            when (page) {
                Page.Settings -> "/settings"
                Page.Identity -> "/settings/identities"
            },
            options
        )
    }

    @Suppress("RedundantSuspendModifier", "BlockingMethodInNonBlockingContext")
    suspend fun promoteAnonymousUser(options: PromoteOptions): AuthorizeResult {
        requireIsInitialized()
        val keyId = tokenRepo.getAnonymousKeyId(name)
            ?: throw AnonymousUserNotFoundException()
        val keyPair = keyRepo.getAnonymousKey(keyId)
            ?: throw AnonymousUserNotFoundException()
        val challenge = oauthRepo.oauthChallenge("anonymous_request").token

        val jwk = publicKeyToJWK(keyId, keyPair.public)

        val header = JWTHeader(
            typ = JWTHeaderType.ANONYMOUS,
            kid = jwk.kid,
            alg = jwk.alg,
            jwk = jwk
        )
        val payload = JWTPayload(
            now = Instant.now(),
            challenge = challenge,
            action = "promote",
            deviceInfo = getDeviceInfo(this.application)

        )
        val signature = makeSignature(keyPair.private)
        val jwt = signJWT(signature, header, payload)
        val loginHint = "https://authgear.com/login_hint?type=anonymous&jwt=${
        URLEncoder.encode(
            jwt,
            StandardCharsets.UTF_8.name()
        )
        }"

        val codeVerifier = this.setupVerifier()

        val authorizeUrl = authorizeEndpoint(
            OIDCAuthenticationRequest(
                redirectUri = options.redirectUri,
                responseType = "code",
                scope = listOf("openid", "offline_access", "https://authgear.com/scopes/full-access"),
                prompt = listOf(PromptOption.LOGIN),
                loginHint = loginHint,
                state = options.state,
                uiLocales = options.uiLocales,
                wechatRedirectURI = options.wechatRedirectURI,
                suppressIDPSessionCookie = shouldSuppressIDPSessionCookie()
            ),
            codeVerifier
        )
        val deepLink = openAuthorizeUrl(options.redirectUri, authorizeUrl)
        val result = finishAuthorization(deepLink)
        tokenRepo.deleteAnonymousKeyId(name)
        return result
    }

    suspend fun fetchUserInfo(): UserInfo {
        requireIsInitialized()
        refreshAccessTokenIfNeeded()

        val accessToken: String = this.accessToken
            ?: throw UnauthenticatedUserException()
        return oauthRepo.oidcUserInfoRequest(accessToken ?: "")
    }

    suspend fun refreshIDToken() {
        requireIsInitialized()
        refreshAccessTokenIfNeeded()

        val accessToken: String = this.accessToken
            ?: throw UnauthenticatedUserException()

        val tokenResponse = oauthRepo.oidcTokenRequest(
            OIDCTokenRequest(
                grantType = com.oursky.authgear.GrantType.ID_TOKEN,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                accessToken = accessToken
            )
        )

        if (tokenResponse.idToken != null) {
            this.idToken = tokenResponse.idToken
        }
    }

    suspend fun refreshAccessTokenIfNeeded(): String? {
        requireIsInitialized()
        if (shouldRefreshAccessToken()) {
            refreshAccessToken()
        }
        return accessToken
    }

    fun clearSessionState() {
        requireIsInitialized()
        clearSession(SessionStateChangeReason.CLEAR)
    }

    private fun updateSessionState(state: SessionState, reason: SessionStateChangeReason) {
        // TODO: Add re-entry detection
        sessionState = state
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            this.delegate?.onSessionStateChanged(this.authgear, reason)
        }
    }

    private fun authorizeEndpoint(request: OIDCAuthenticationRequest, codeVerifier: Verifier?): String {
        val config = oauthRepo.getOIDCConfiguration()
        val query = request.toQuery(this.clientId, codeVerifier)
        return "${config.authorizationEndpoint}?${query.toQueryParameter()}"
    }

    private fun setupVerifier(): Verifier {
        val verifier = generateCodeVerifier()
        tokenRepo.setOIDCCodeVerifier(name, verifier)
        return Verifier(verifier, computeCodeChallenge(verifier))
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString(separator = "") {
            it.toString(16).padStart(2, '0')
        }
    }

    private fun computeCodeChallenge(verifier: String): String {
        val hash = sha256(verifier)
        return String(
            Base64.encode(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
            StandardCharsets.UTF_8
        )
    }

    private fun sha256(input: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(input.toByteArray(StandardCharsets.UTF_8))
        return md.digest()
    }

    private fun shouldRefreshAccessToken(): Boolean {
        if (refreshToken == null) return false
        if (accessToken == null) return true
        val expireAt = this.expireAt ?: return true
        val now = Instant.now()
        if (expireAt.isBefore(now)) return true
        return false
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun refreshAccessToken() {
        coroutineScope {
            val job = async(start = CoroutineStart.LAZY) {
                doRefreshAccessToken()
            }
            if (refreshAccessTokenJob.compareAndSet(null, job)) {
                job.await()
                refreshAccessTokenJob.set(null)
            } else {
                job.cancel()
                // Another thread already started refreshing access token. Try to await.
                val existingJob = refreshAccessTokenJob.get()
                if (existingJob == null) {
                    // The job had finished.
                    return@coroutineScope
                } else {
                    existingJob.join()
                }
            }
        }
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun doRefreshAccessToken() {
        val refreshToken = refreshTokenRepo.getRefreshToken(name)
        if (refreshToken == null) {
            // Somehow we are asked to refresh access token but we don't have the refresh token.
            // Something went wrong, clear session.
            clearSession(SessionStateChangeReason.NO_TOKEN)
            return
        }
        val tokenResponse: OIDCTokenResponse?
        try {
            tokenResponse = oauthRepo.oidcTokenRequest(
                OIDCTokenRequest(
                    grantType = GrantType.REFRESH_TOKEN,
                    clientId = clientId,
                    xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                    refreshToken = refreshToken
                )
            )
        } catch (e: Exception) {
            if (e is OAuthException && e.error == "invalid_grant") {
                clearSession(SessionStateChangeReason.INVALID)
                return
            }
            throw e
        }
        saveToken(tokenResponse, SessionStateChangeReason.FOUND_TOKEN)
    }

    private fun saveToken(tokenResponse: OIDCTokenResponse, reason: SessionStateChangeReason) {
        synchronized(this) {
            if (tokenResponse.accessToken != null) {
                accessToken = tokenResponse.accessToken
            }
            if (tokenResponse.refreshToken != null) {
                refreshToken = tokenResponse.refreshToken
            }
            if (tokenResponse.idToken != null) {
                idToken = tokenResponse.idToken
            }
            if (tokenResponse.expiresIn != null) {
                expireAt =
                    Instant.now() + Duration.ofMillis((tokenResponse.expiresIn * ExpireInPercentage).toLong())
            }
            updateSessionState(SessionState.AUTHENTICATED, reason)
        }
        val refreshToken = this.refreshToken
        if (refreshToken != null) {
            refreshTokenRepo.setRefreshToken(name, refreshToken)
        }
    }

    private fun clearSession(changeReason: SessionStateChangeReason) {
        tokenRepo.deleteRefreshToken(name)
        synchronized(this) {
            accessToken = null
            refreshToken = null
            idToken = null
            expireAt = null
            updateSessionState(SessionState.NO_SESSION, changeReason)
        }
    }

    private fun shouldSuppressIDPSessionCookie(): Boolean {
        return !this.shareSessionWithDeviceBrowser
    }

    private suspend fun openAuthorizeUrl(
        redirectUrl: String,
        authorizeUrl: String
    ): String {
        val existingHandler = DeepLinkHandlerMap[redirectUrl]
        require(existingHandler == null) {
            "The redirect url $redirectUrl is already being handled by ${existingHandler?.name} when $name attempts to handle it"
        }
        return suspendCoroutine {
            DeepLinkHandlerMap[redirectUrl] = SuspendHolder(name, it)
            application.startActivity(
                OauthActivity.createAuthorizationIntent(
                    application,
                    redirectUrl,
                    authorizeUrl
                )
            )
        }
    }

    private fun finishAuthorization(deepLink: String): AuthorizeResult {
        val uri = Uri.parse(deepLink)
        val redirectUri = "${uri.scheme}://${uri.authority}${uri.path}"
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        var errorURI = uri.getQueryParameter("error_uri")
        if (error != null) {
            throw OAuthException(
                error = error,
                errorDescription = errorDescription,
                state = state,
                errorURI = errorURI
            )
        }
        val code = uri.getQueryParameter("code")
            ?: throw OAuthException(
                error = "invalid_request",
                errorDescription = "Missing parameter: code",
                state = state,
                errorURI = errorURI
            )
        val codeVerifier = tokenRepo.getOIDCCodeVerifier(name)
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OIDCTokenRequest(
                grantType = GrantType.AUTHORIZATION_CODE,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier ?: ""
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(tokenResponse.accessToken!!)
        saveToken(tokenResponse, SessionStateChangeReason.AUTHENTICATED)
        disableBiometric()
        return AuthorizeResult(userInfo, uri.getQueryParameter("state"))
    }

    private fun finishReauthentication(deepLink: String): ReauthenticateResult {
        val uri = Uri.parse(deepLink)
        val redirectUri = "${uri.scheme}://${uri.authority}${uri.path}"
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        var errorURI = uri.getQueryParameter("error_uri")
        if (error != null) {
            throw OAuthException(
                error = error,
                errorDescription = errorDescription,
                state = state,
                errorURI = errorURI
            )
        }
        val code = uri.getQueryParameter("code")
            ?: throw OAuthException(
                error = "invalid_request",
                errorDescription = "Missing parameter: code",
                state = state,
                errorURI = errorURI
            )
        val codeVerifier = tokenRepo.getOIDCCodeVerifier(name)
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OIDCTokenRequest(
                grantType = GrantType.AUTHORIZATION_CODE,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier ?: ""
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(tokenResponse.accessToken!!)
        tokenResponse.idToken?.let {
            this.idToken = it
        }
        return ReauthenticateResult(userInfo, uri.getQueryParameter("state"))
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun checkBiometricSupported(context: Context, allowed: Int) {
        requireIsInitialized()
        requireMinimumBiometricAPILevel()

        var allowed = allowed
        ensureAllowedIsValid(allowed)
        allowed = convertAllowed(allowed)
        val result = BiometricManager.from(context).canAuthenticate(allowed)
        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            throw wrapException(BiometricCanAuthenticateException(result))
        }
    }

    fun isBiometricEnabled(): Boolean {
        requireIsInitialized()

        val kid = this.tokenRepo.getBiometricKeyId(this.name)
        if (kid == null) {
            return false
        }
        return true
    }

    fun disableBiometric() {
        requireIsInitialized()

        val kid = this.tokenRepo.getBiometricKeyId(this.name)
        if (kid != null) {
            val alias = "com.authgear.keys.biometric.$kid"
            removePrivateKey(alias)
            this.tokenRepo.deleteBiometricKeyId(this.name)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    suspend fun enableBiometric(
        options: BiometricOptions
    ) {
        requireIsInitialized()
        requireMinimumBiometricAPILevel()
        refreshAccessTokenIfNeeded()

        val accessToken: String = this.accessToken
            ?: throw UnauthenticatedUserException()

        ensureAllowedIsValid(options.allowedAuthenticators)
        val allowed = convertAllowed(options.allowedAuthenticators)
        val promptInfo = buildPromptInfo(
            options.title,
            options.subtitle,
            options.description,
            options.negativeButtonText,
            allowed
        )

        val kid = UUID.randomUUID().toString()
        val alias = "com.authgear.keys.biometric.$kid"
        val spec = makeGenerateKeyPairSpec(alias, authenticatorTypesToKeyProperties(allowed), options.invalidatedByBiometricEnrollment)
        val challenge = this.oauthRepo.oauthChallenge("biometric_request").token
        val keyPair = createKeyPair(spec)
        val jwk = publicKeyToJWK(kid, keyPair.public)
        val header = JWTHeader(
            typ = JWTHeaderType.BIOMETRIC,
            kid = kid,
            alg = jwk.alg,
            jwk = jwk
        )
        val payload = JWTPayload(
            now = Instant.now(),
            challenge = challenge,
            action = "setup",
            deviceInfo = getDeviceInfo(this.application)
        )
        val lockedSignature = makeSignature(keyPair.private)
        val cryptoObject = BiometricPrompt.CryptoObject(lockedSignature)

        val jwt = suspendCoroutine<String> {
            val prompt =
                BiometricPrompt(
                    options.activity,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationFailed() {
                            // This callback will be invoked EVERY time the recognition failed.
                            // So while the prompt is still opened, this callback can be called repetitively.
                            // Finally, either onAuthenticationError or onAuthenticationSucceeded will be called.
                            // So this callback is not important to the developer.
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            it.resumeWith(
                                Result.failure(
                                    wrapException(BiometricPromptAuthenticationException(
                                        errorCode
                                    ))
                                )
                            )
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            val signature = result.cryptoObject!!.signature!!
                            val jwt = signJWT(signature, header, payload)
                            it.resume(jwt)
                        }
                    })

            val handler = Handler(Looper.getMainLooper())
            handler.post {
                prompt.authenticate(promptInfo, cryptoObject)
            }
        }

        this.oauthRepo.biometricSetupRequest(accessToken, clientId, jwt)
        tokenRepo.setBiometricKeyId(name, kid)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    suspend fun authenticateBiometric(
        options: BiometricOptions
    ): UserInfo {
        requireIsInitialized()
        requireMinimumBiometricAPILevel()

        ensureAllowedIsValid(options.allowedAuthenticators)
        val allowed = convertAllowed(options.allowedAuthenticators)
        val promptInfo = buildPromptInfo(
            options.title,
            options.subtitle,
            options.description,
            options.negativeButtonText,
            allowed
        )

        val challenge = this.oauthRepo.oauthChallenge("biometric_request").token
        val kid = tokenRepo.getBiometricKeyId(name)
            ?: throw BiometricPrivateKeyNotFoundException()
        val alias = "com.authgear.keys.biometric.$kid"

        try {
            val keyPair =
                getPrivateKey(alias) ?: throw BiometricPrivateKeyNotFoundException()
            val jwk = publicKeyToJWK(kid, keyPair.public)
            val header = JWTHeader(
                typ = JWTHeaderType.BIOMETRIC,
                kid = kid,
                alg = jwk.alg,
                jwk = jwk
            )
            val payload = JWTPayload(
                now = Instant.now(),
                challenge = challenge,
                action = "authenticate",
                deviceInfo = getDeviceInfo(this.application)
            )
            val lockedSignature = makeSignature(keyPair.private)
            val cryptoObject = BiometricPrompt.CryptoObject(lockedSignature)

            val jwt = suspendCoroutine<String> {
                val prompt =
                    BiometricPrompt(
                        options.activity,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationFailed() {
                                // This callback will be invoked EVERY time the recognition failed.
                                // So while the prompt is still opened, this callback can be called repetitively.
                                // Finally, either onAuthenticationError or onAuthenticationSucceeded will be called.
                                // So this callback is not important to the developer.
                            }

                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence
                            ) {
                                it.resumeWith(
                                    Result.failure(
                                        wrapException(BiometricPromptAuthenticationException(
                                            errorCode
                                        ))
                                    )
                                )
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                val signature = result.cryptoObject!!.signature!!
                                val jwt = signJWT(signature, header, payload)
                                it.resume(jwt)
                            }
                        })

                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    prompt.authenticate(promptInfo, cryptoObject)
                }
            }

            try {
                val tokenResponse = oauthRepo.oidcTokenRequest(
                    OIDCTokenRequest(
                        grantType = com.oursky.authgear.GrantType.BIOMETRIC,
                        clientId = clientId,
                        xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                        jwt = jwt
                    )
                )
                val userInfo = oauthRepo.oidcUserInfoRequest(
                    tokenResponse.accessToken!!
                )
                saveToken(tokenResponse, SessionStateChangeReason.AUTHENTICATED)
                return userInfo
            } catch (e: OAuthException) {
                // In case the biometric was removed remotely.
                if (e.error == "invalid_grant" && e.errorDescription == "InvalidCredentials") {
                    disableBiometric()
                }
                throw e
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            // This biometric has changed
            this.disableBiometric()
            throw wrapException(e)
        } catch (e: Exception) {
            throw wrapException(e)
        }
    }
}
