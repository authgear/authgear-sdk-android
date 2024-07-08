package com.oursky.authgear

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.oursky.authgear.app2app.App2App
import com.oursky.authgear.app2app.App2AppAuthenticateOptions
import com.oursky.authgear.app2app.App2AppAuthenticateRequest
import com.oursky.authgear.app2app.App2AppOptions
import com.oursky.authgear.data.assetlink.AssetLinkRepo
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.data.oauth.OAuthRepo
import com.oursky.authgear.net.toQueryParameter
import com.oursky.authgear.oauth.OidcAuthenticationRequest
import com.oursky.authgear.oauth.OidcTokenRequest
import com.oursky.authgear.oauth.OidcTokenResponse
import com.oursky.authgear.oauth.toQuery
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Listeners should only be set on the main thread.
 */
internal class AuthgearCore(
    private val authgear: Authgear,
    val application: Application,
    val clientId: String,
    private val authgearEndpoint: String,
    private val isSsoEnabled: Boolean,
    private val app2AppOptions: App2AppOptions,
    private val tokenStorage: TokenStorage,
    private val uiImplementation: UIImplementation,
    private val storage: ContainerStorage,
    private val oauthRepo: OAuthRepo,
    private val keyRepo: KeyRepo,
    private val assetLinkRepo: AssetLinkRepo,
    name: String? = null
) {
    companion object {
        @Suppress("unused")
        private val TAG = AuthgearCore::class.java.simpleName

        /**
         * To prevent user from using expired access token, we have to check in advance
         * whether it had expired and refresh it accordingly in [refreshAccessTokenIfNeeded]. If we
         * use the expiry time in [OidcTokenResponse] directly to check for expiry, it is possible
         * that the access token had passed the check but ends up being expired when it arrives at
         * the server due to slow traffic or unfair scheduler.
         *
         * To compat this, we should consider the access token expired earlier than the expiry time
         * calculated using [OidcTokenResponse.expiresIn]. Current implementation uses
         * [EXPIRE_IN_PERCENTAGE] of [OidcTokenResponse.expiresIn] to calculate the expiry time.
         */
        private const val EXPIRE_IN_PERCENTAGE = 0.9

        const val KEY_OAUTH_BOARDCAST_TYPE = "boardcastType"
        const val KEY_REDIRECT_URL = "redirectUrl"
        const val CODE_CHALLENGE_METHOD = "S256"

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

    private val app2app: App2App = App2App(
        application,
        this.name,
        storage,
        oauthRepo,
        keyRepo,
        assetLinkRepo
    )

    init {
        oauthRepo.endpoint = authgearEndpoint

        if (app2AppOptions.isEnabled) {
            requireMinimumApp2AppAPILevel()
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

    private fun requireMinimumApp2AppAPILevel() {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            "App2App authentication requires at least API Level 23"
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun authenticateAnonymously(): UserInfo {
        requireIsInitialized()
        val challenge = oauthRepo.oauthChallenge("anonymous_request").token

        var keyId = storage.getAnonymousKeyId(name)
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
            OidcTokenRequest(
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
        storage.setAnonymousKeyId(name, keyId)
        return userInfo
    }

    @ExperimentalAuthgearApi
    @Suppress("RedundantSuspendModifier")
    suspend fun createAuthenticateRequest(
        options: AuthenticateOptions,
        verifier: Verifier = generateCodeVerifier()
    ): AuthenticationRequest {
        requireIsInitialized()
        val request = options.toRequest(this.isSsoEnabled)
        val authorizeUri = authorizeEndpoint(request, verifier)
        return AuthenticationRequest(authorizeUri, request.redirectUri, verifier)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(ExperimentalAuthgearApi::class)
    suspend fun authenticate(options: AuthenticateOptions): UserInfo {
        requireIsInitialized()
        val codeVerifier = this.setupVerifier()
        val request = createAuthenticateRequest(options, codeVerifier)
        val deepLink = openAuthorizeUrl(request.redirectUri, request.url)
        return finishAuthorization(deepLink, codeVerifier)
    }

    @Suppress("RedundantSuspendModifier")
    @OptIn(ExperimentalAuthgearApi::class)
    private suspend fun createSettingsActionRequest(
        action: SettingsAction,
        options: SettingsActionOptions,
        verifier: Verifier = generateCodeVerifier()
    ): AuthenticationRequest {
        requireIsInitialized()

        val refreshToken = tokenStorage.getRefreshToken(name)
            ?: throw UnauthenticatedUserException()

        val token: String
        try {
            token = oauthRepo.oauthAppSessionToken(refreshToken).appSessionToken
        } catch (e: Exception) {
            handleInvalidGrantError(e)
            throw e
        }

        val loginHint = "https://authgear.com/login_hint?type=app_session_token&app_session_token=${
            URLEncoder.encode(token, StandardCharsets.UTF_8.name())
        }"

        val idTokenHint = this.idToken ?: throw AuthgearException("Call refreshIDToken first")

        val request = options.toRequest(action = action, idTokenHint = idTokenHint, loginHint = loginHint)
        val authorizeUri = authorizeEndpoint(request, verifier)
        return AuthenticationRequest(authorizeUri, request.redirectUri, verifier)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(ExperimentalAuthgearApi::class)
    internal suspend fun settingsAction(action: SettingsAction, options: SettingsActionOptions) {
        requireIsInitialized()
        val codeVerifier = this.setupVerifier()
        val request = createSettingsActionRequest(action, options, codeVerifier)
        val deepLink = openAuthorizeUrl(request.redirectUri, request.url)
        return finishSettingsAction(deepLink, codeVerifier)
    }

    @ExperimentalAuthgearApi
    @Suppress("RedundantSuspendModifier")
    suspend fun createReauthenticateRequest(
        options: ReauthenticateOptions,
        verifier: Verifier = generateCodeVerifier()
    ): AuthenticationRequest {
        requireIsInitialized()
        val idTokenHint = this.idToken ?: throw AuthgearException("Call refreshIDToken first")
        val request = options.toRequest(idTokenHint, this.isSsoEnabled)
        val authorizeUrl = authorizeEndpoint(request, verifier)
        return AuthenticationRequest(authorizeUrl, request.redirectUri, verifier)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @OptIn(ExperimentalAuthgearApi::class)
    suspend fun reauthenticate(
        options: ReauthenticateOptions,
        biometricOptions: BiometricOptions?
    ): UserInfo {
        requireIsInitialized()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val biometricEnabled = this.isBiometricEnabled()
            if (biometricEnabled && biometricOptions != null) {
                return this.authenticateBiometric(biometricOptions)
            }
        }

        if (!this.canReauthenticate) {
            throw AuthgearException("canReauthenticate is false")
        }

        val codeVerifier = this.setupVerifier()
        val request = createReauthenticateRequest(options, codeVerifier)
        val deepLink = openAuthorizeUrl(request.redirectUri, request.url)
        return finishReauthentication(deepLink)
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun configure() {
        isInitialized = true
        val refreshToken = tokenStorage.getRefreshToken(name)
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
            val refreshToken = tokenStorage.getRefreshToken(name) ?: ""
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

    suspend fun generateAuthgearUrl(
        path: String,
        options: SettingOptions? = null
    ): Uri {
        requireIsInitialized()

        val oidcConfig = oauthRepo.getOidcConfiguration()
        val authzUri = Uri.parse(oidcConfig.authorizationEndpoint)
        val builder = authzUri.getOrigin()?.let {
            Uri.parse(it).buildUpon()
        } ?: throw AuthgearException("invalid authorization_endpoint")
        builder.path(path)
        options?.colorScheme?.let {
            builder.appendQueryParameter("x_color_scheme", it.raw)
        }
        options?.uiLocales?.let {
            builder.appendQueryParameter("ui_locales", it.joinToString(" "))
        }
        val redirectUri = builder.build()
        return generateUrl(redirectUri.toString(), options)
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun generateUrl(redirectUri: String, options: SettingOptions? = null): Uri {
        requireIsInitialized()

        val refreshToken = tokenStorage.getRefreshToken(name)
            ?: throw UnauthenticatedUserException()

        val token: String
        try {
            token = oauthRepo.oauthAppSessionToken(refreshToken).appSessionToken
        } catch (e: Exception) {
            handleInvalidGrantError(e)
            throw e
        }

        val loginHint = "https://authgear.com/login_hint?type=app_session_token&app_session_token=${
            URLEncoder.encode(token, StandardCharsets.UTF_8.name())
        }"

        return authorizeEndpoint(
            OidcAuthenticationRequest(
                redirectUri = redirectUri,
                responseType = "none",
                scope = listOf("openid", "offline_access", "https://authgear.com/scopes/full-access"),
                isSsoEnabled = this.isSsoEnabled,
                prompt = listOf(PromptOption.NONE),
                loginHint = loginHint,
                wechatRedirectURI = options?.wechatRedirectURI,
                uiLocales = options?.uiLocales,
                colorScheme = options?.colorScheme
            ),
            null
        )
    }

    suspend fun openUrl(path: String, options: SettingOptions? = null) {
        requireIsInitialized()

        val authorizeUrl = generateAuthgearUrl(
            path = path,
            options = options
        )

        return suspendCoroutine { k ->
            val action = newRandomAction()
            val intentFilter = IntentFilter(action)
            val br = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val type = intent?.getStringExtra(WebViewActivity.KEY_BROADCAST_TYPE) ?: return
                    when (type) {
                        WebViewActivity.BroadcastType.END.name -> {
                            application.unregisterReceiver(this)
                            k.resume(Unit)
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                application.registerReceiver(br, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                application.registerReceiver(br, intentFilter)
            }
            application.startActivity(
                WebViewActivity.createIntent(application, action, authorizeUrl)
            )
        }
    }

    suspend fun open(page: Page, options: SettingOptions? = null) {
        openUrl(
            when (page) {
                Page.SETTINGS -> "/settings"
                Page.IDENTITY -> "/settings/identities"
            },
            options
        )
    }

    @Suppress("RedundantSuspendModifier", "BlockingMethodInNonBlockingContext")
    suspend fun promoteAnonymousUser(options: PromoteOptions): UserInfo {
        requireIsInitialized()
        val keyId = storage.getAnonymousKeyId(name)
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
            OidcAuthenticationRequest(
                redirectUri = options.redirectUri,
                responseType = "code",
                scope = listOf("openid", "offline_access", "https://authgear.com/scopes/full-access"),
                isSsoEnabled = this.isSsoEnabled,
                prompt = listOf(PromptOption.LOGIN),
                loginHint = loginHint,
                state = options.state,
                xState = options.xState,
                uiLocales = options.uiLocales,
                colorScheme = options.colorScheme,
                wechatRedirectURI = options.wechatRedirectURI
            ),
            codeVerifier
        )
        val deepLink = openAuthorizeUrl(options.redirectUri, authorizeUrl)
        val result = finishAuthorization(deepLink)
        storage.deleteAnonymousKeyId(name)
        return result
    }

    suspend fun fetchUserInfo(): UserInfo {
        requireIsInitialized()
        refreshAccessTokenIfNeeded()

        val accessToken: String = this.accessToken
            ?: throw UnauthenticatedUserException()
        try {
            return oauthRepo.oidcUserInfoRequest(accessToken ?: "")
        } catch (e: Exception) {
            handleInvalidGrantError(e)
            throw e
        }
    }

    suspend fun refreshIDToken() {
        requireIsInitialized()
        refreshAccessTokenIfNeeded()

        val accessToken: String = this.accessToken
            ?: throw UnauthenticatedUserException()

        try {
            val tokenResponse = oauthRepo.oidcTokenRequest(
                OidcTokenRequest(
                    grantType = com.oursky.authgear.GrantType.ID_TOKEN,
                    clientId = clientId,
                    xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                    accessToken = accessToken
                )
            )

            if (tokenResponse.idToken != null) {
                this.idToken = tokenResponse.idToken
            }
        } catch (e: Exception) {
            handleInvalidGrantError(e)
            throw e
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

    private fun authorizeEndpoint(request: OidcAuthenticationRequest, codeVerifier: Verifier?): Uri {
        val config = oauthRepo.getOidcConfiguration()
        val query = request.toQuery(this.clientId, codeVerifier)
        return Uri.parse(config.authorizationEndpoint).buildUpon().let {
            it.encodedQuery(query.toQueryParameter())
        }.build()
    }

    private fun setupVerifier(): Verifier {
        val verifier = generateCodeVerifier()
        // TODO: need store verifier?
        storage.setOidcCodeVerifier(name, verifier.verifier)
        return verifier
    }

    private fun generateCodeVerifier(): Verifier {
        // https://datatracker.ietf.org/doc/html/rfc7636#section-4.1
        // It is RECOMMENDED that the output of
        // a suitable random number generator be used to create a 32-octet
        // sequence.  The octet sequence is then base64url-encoded to produce a
        // 43-octet URL safe string to use as the code verifier.
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val verifier = base64UrlEncode(bytes)
        return Verifier(verifier, computeCodeChallenge(verifier))
    }

    private fun computeCodeChallenge(verifier: String): String {
        val hash = sha256(verifier)
        return base64UrlEncode(hash)
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
        val refreshToken = tokenStorage.getRefreshToken(name)
        if (refreshToken == null) {
            // Somehow we are asked to refresh access token but we don't have the refresh token.
            // Something went wrong, clear session.
            clearSession(SessionStateChangeReason.NO_TOKEN)
            return
        }
        val tokenResponse: OidcTokenResponse?
        try {
            tokenResponse = oauthRepo.oidcTokenRequest(
                OidcTokenRequest(
                    grantType = GrantType.REFRESH_TOKEN,
                    clientId = clientId,
                    xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                    refreshToken = refreshToken
                )
            )
        } catch (e: Exception) {
            handleInvalidGrantError(e)
            if (e is OAuthException && e.error == "invalid_grant") {
                return
            }
            throw e
        }
        saveToken(tokenResponse, SessionStateChangeReason.FOUND_TOKEN)
    }

    private fun saveToken(tokenResponse: OidcTokenResponse, reason: SessionStateChangeReason) {
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
                    Instant.now() + Duration.ofSeconds((tokenResponse.expiresIn * EXPIRE_IN_PERCENTAGE).toLong())
            }
            updateSessionState(SessionState.AUTHENTICATED, reason)
        }
        val refreshToken = this.refreshToken
        if (refreshToken != null) {
            tokenStorage.setRefreshToken(name, refreshToken)
        }
    }

    private fun clearSession(changeReason: SessionStateChangeReason) {
        tokenStorage.deleteRefreshToken(name)
        storage.deleteApp2AppDeviceKeyId(name)
        synchronized(this) {
            accessToken = null
            refreshToken = null
            idToken = null
            expireAt = null
            updateSessionState(SessionState.NO_SESSION, changeReason)
        }
    }

    private suspend fun openAuthorizeUrl(
        redirectUrl: String,
        authorizeUri: Uri
    ): String {
        return suspendCoroutine {
            val options = OpenAuthorizationURLOptions(authorizeUri, Uri.parse(redirectUrl))
            val listener = object : OnOpenAuthorizationURLListener {
                override fun onSuccess(url: Uri) {
                    it.resume(url.toString())
                }

                override fun onFailed(throwable: Throwable) {
                    it.resumeWithException(throwable)
                }
            }
            this.uiImplementation.openAuthorizationURL(this.application, options, listener)
        }
    }

    fun finishAuthorization(deepLink: String, verifier: Verifier? = null): UserInfo {
        val uri = Uri.parse(deepLink)
        val redirectUri = "${uri.scheme}://${uri.authority}${uri.path}"
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        var errorURI = uri.getQueryParameter("error_uri")
        if (error != null) {
            if (error == "cancel") {
                throw CancelException()
            }
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
        val codeVerifier = verifier?.verifier ?: storage.getOidcCodeVerifier(name)
        var app2appJwt: String? = null
        if (app2AppOptions.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            app2appJwt = app2app.generateApp2AppJWT(forceNewKey = true)
        }
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OidcTokenRequest(
                grantType = GrantType.AUTHORIZATION_CODE,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier ?: "",
                xApp2AppDeviceKeyJwt = app2appJwt
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(tokenResponse.accessToken!!)
        saveToken(tokenResponse, SessionStateChangeReason.AUTHENTICATED)
        disableBiometric()
        return userInfo
    }

    private fun finishSettingsAction(deepLink: String, verifier: Verifier? = null) {
        val uri = Uri.parse(deepLink)
        val redirectUri = "${uri.scheme}://${uri.authority}${uri.path}"
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        var errorURI = uri.getQueryParameter("error_uri")
        if (error != null) {
            if (error == "cancel") {
                throw CancelException()
            }
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
        val codeVerifier = verifier?.verifier ?: storage.getOidcCodeVerifier(name)
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OidcTokenRequest(
                grantType = GrantType.SETTINGS_ACTION,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier ?: "",
            )
        )
        disableBiometric()
    }

    fun finishReauthentication(deepLink: String, verifier: Verifier? = null): UserInfo {
        val uri = Uri.parse(deepLink)
        val redirectUri = "${uri.scheme}://${uri.authority}${uri.path}"
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        var errorURI = uri.getQueryParameter("error_uri")
        if (error != null) {
            if (error == "cancel") {
                throw CancelException()
            }
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
        val codeVerifier = verifier?.verifier ?: storage.getOidcCodeVerifier(name)
        var app2appJwt: String? = null
        if (app2AppOptions.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            app2appJwt = app2app.generateApp2AppJWT(forceNewKey = false)
        }
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OidcTokenRequest(
                grantType = GrantType.AUTHORIZATION_CODE,
                clientId = clientId,
                xDeviceInfo = getDeviceInfo(this.application).toBase64URLEncodedString(),
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier ?: "",
                xApp2AppDeviceKeyJwt = app2appJwt
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(tokenResponse.accessToken!!)
        tokenResponse.idToken?.let {
            this.idToken = it
        }
        return userInfo
    }

    private fun handleInvalidGrantError(e: Exception) {
        if (e is OAuthException && e.error == "invalid_grant") {
            clearSession(SessionStateChangeReason.INVALID)
            return
        } else if (e is ServerException && e.reason == "InvalidGrant") {
            clearSession(SessionStateChangeReason.INVALID)
            return
        }
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

        val kid = this.storage.getBiometricKeyId(this.name)
        if (kid == null) {
            return false
        }
        return true
    }

    fun disableBiometric() {
        requireIsInitialized()

        val kid = this.storage.getBiometricKeyId(this.name)
        if (kid != null) {
            val alias = "com.authgear.keys.biometric.$kid"
            removePrivateKey(alias)
            this.storage.deleteBiometricKeyId(this.name)
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
        val spec = makeGenerateKeyPairSpec(
            alias,
            authenticatorTypesToKeyProperties(allowed),
            options.invalidatedByBiometricEnrollment)
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

        try {
            this.oauthRepo.biometricSetupRequest(accessToken, clientId, jwt)
            storage.setBiometricKeyId(name, kid)
        } catch (e: Exception) {
            handleInvalidGrantError(e)
            throw e
        }
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
        val kid = storage.getBiometricKeyId(name)
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
                    OidcTokenRequest(
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    suspend fun startApp2AppAuthentication(options: App2AppAuthenticateOptions): UserInfo {
        requireIsInitialized()
        requireMinimumApp2AppAPILevel()
        val verifier = setupVerifier()
        val request = app2app.createAuthenticateRequest(
            clientID = clientId,
            options = options,
            verifier = verifier
        )
        val resultURI = app2app.startAuthenticateRequest(request)
        return finishAuthorization(resultURI.toString(), verifier)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun parseApp2AppAuthenticationRequest(uri: Uri): App2AppAuthenticateRequest? {
        requireMinimumApp2AppAPILevel()
        return app2app.parseApp2AppAuthenticationRequest(uri)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun approveApp2AppAuthenticationRequest(request: App2AppAuthenticateRequest) {
        requireMinimumApp2AppAPILevel()
        return app2app.approveApp2AppAuthenticationRequest(refreshToken, request)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun rejectApp2AppAuthenticationRequest(request: App2AppAuthenticateRequest, reason: Throwable) {
        requireMinimumApp2AppAPILevel()
        return app2app.rejectApp2AppAuthenticationRequest(request, reason)
    }
}
