package com.oursky.authgear

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.annotation.MainThread
import androidx.browser.customtabs.CustomTabsIntent
import com.oursky.authgear.data.key.JwkResponse
import com.oursky.authgear.data.key.KeyRepo
import com.oursky.authgear.data.oauth.OauthRepo
import com.oursky.authgear.data.token.TokenRepo
import com.oursky.authgear.jwt.prepareJwtData
import com.oursky.authgear.net.toQueryParameter
import com.oursky.authgear.oauth.OIDCTokenRequest
import com.oursky.authgear.oauth.OIDCTokenResponse
import com.oursky.authgear.oauth.OauthException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Collections.synchronizedList
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

        /**
         * A map used to keep track of which deep link is being handled by which container.
         */
        private val DeepLinkHandlerMap = mutableMapOf<String, SuspendHolder<String>>()
        fun handleDeepLink(deepLink: String, isSuccessful: Boolean) {
            if (isSuccessful) {
                // The deep link would contain code in query parameter so we trim it to get back the handler.
                val uri = Uri.parse(deepLink)
                val deepLinkWithoutQuery = "${uri.scheme}://${uri.authority}${uri.path}"
                val handler = requireDeepLinkHandler(deepLinkWithoutQuery)
                handler.continuation.resume(deepLink)
                DeepLinkHandlerMap.remove(deepLinkWithoutQuery)
            } else {
                val handler = requireDeepLinkHandler(deepLink)
                handler.continuation.resumeWith(Result.failure(CancelledException()))
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
    }

    data class SuspendHolder<T>(val name: String, val continuation: Continuation<T>)

    data class Verifier(
        val verifier: String,
        val challenge: String
    )

    init {
        oauthRepo.endpoint = authgearEndpoint
    }

    private val name = name ?: "default"
    private var isInitialized = false
    private var refreshToken: String? = null
    var accessToken: String? = null
        private set
    private var expireAt: Instant? = null
    var sessionState: SessionState = SessionState.Unknown
        private set
    private val refreshAccessTokenJob = AtomicReference<Job>(null)
    private val onSessionStateChangedListeners =
        synchronizedList(mutableListOf<ListenerPair<OnSessionStateChangedListener>>())

    private fun requireIsMainThread() {
        require(Looper.myLooper() == Looper.getMainLooper()) {
            "Listener should only be set on the main thread"
        }
    }

    private fun requireIsInitialized() {
        require(isInitialized) {
            "Authgear is not configured. Did you forget to call configure?"
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun authenticateAnonymously(): UserInfo {
        requireIsInitialized()
        val token = oauthRepo.oauthChallenge("anonymous_request").token
        val keyId = tokenRepo.getAnonymousKeyId(name)
        val key = keyRepo.getAnonymousKey(keyId)
        val now = Instant.now().epochSecond
        val header = key.toHeader()
        val payload = mutableMapOf<String, String>()
        payload["iat"] = now.toString()
        payload["exp"] = (now + 60).toString()
        payload["challenge"] = token
        payload["action"] = "auth"
        val jwt = getJwt(key.kid, header, payload)
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OIDCTokenRequest(
                grantType = GrantType.ANONYMOUS,
                clientId = clientId,
                jwt = jwt
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(
            tokenResponse.accessToken
        )
        saveToken(tokenResponse, SessionStateChangeReason.Authorized)
        tokenRepo.setAnonymousKeyId(name, key.kid)
        return userInfo
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun authorize(options: AuthorizeOptions): AuthorizeResult {
        requireIsInitialized()
        val authorizeUrl = authorizeEndpoint(options)
        val deepLink = openAuthorizeUrl(options.redirectUri, authorizeUrl)
        return finishAuthorization(deepLink)
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun configure(skipRefreshAccessToken: Boolean = false) {
        isInitialized = true
        val refreshToken = tokenRepo.getRefreshToken(name)
        this.refreshToken = refreshToken
        if (shouldRefreshAccessToken()) {
            if (skipRefreshAccessToken) {
                // Consider user as logged in if refresh token is available
                updateSessionState(SessionState.LoggedIn, SessionStateChangeReason.FoundToken)
            } else {
                refreshAccessToken()
            }
        } else {
            updateSessionState(SessionState.NoSession, SessionStateChangeReason.NoToken)
        }
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun logout(force: Boolean? = null) {
        requireIsInitialized()
        try {
            val refreshToken = tokenRepo.getRefreshToken(name) ?: ""
            oauthRepo.oidcRevocationRequest(refreshToken)
        } catch (e: Exception) {
            if (force != true) {
                throw e
            }
        }
        clearSession(SessionStateChangeReason.Logout)
    }

    @MainThread
    fun openUrl(activity: Activity, path: String) {
        val uri = Uri.parse(URL(URL(authgearEndpoint), path).toString())
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(activity, uri)
    }

    fun open(activity: Activity, page: Page) {
        openUrl(
            activity,
            when (page) {
                Page.Settings -> "/settings"
                Page.Identity -> "/settings/identities"
            }
        )
    }

    @Suppress("RedundantSuspendModifier", "BlockingMethodInNonBlockingContext")
    suspend fun promoteAnonymousUser(options: PromoteOptions): AuthorizeResult {
        requireIsInitialized()
        val keyId = tokenRepo.getAnonymousKeyId(name)
            ?: throw IllegalStateException("Anonymous user credentials not found")
        val key = keyRepo.getAnonymousKey(keyId)
        val token = oauthRepo.oauthChallenge("anonymous_request").token
        val now = Instant.now().epochSecond
        val header = key.toHeader()
        val payload = mutableMapOf<String, String>()
        payload["iat"] = now.toString()
        payload["exp"] = (now + 60).toString()
        payload["challenge"] = token
        payload["action"] = "promote"
        val jwt = getJwt(key.kid, header, payload)
        val loginHint = "https://authgear.com/login_hint?type=anonymous&jwt=${
            URLEncoder.encode(
                jwt,
                StandardCharsets.UTF_8.toString()
            )
        }"
        val authorizeUrl = authorizeEndpoint(
            AuthorizeOptions(
                redirectUri = options.redirectUri,
                prompt = "login",
                loginHint = loginHint,
                state = options.state,
                uiLocales = options.uiLocales
            )
        )
        val deepLink = openAuthorizeUrl(options.redirectUri, authorizeUrl)
        val result = finishAuthorization(deepLink)
        tokenRepo.deleteAnonymousKeyId(name)
        return result
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun fetchUserInfo(): UserInfo {
        requireIsInitialized()
        return oauthRepo.oidcUserInfoRequest(accessToken ?: "")
    }

    suspend fun refreshAccessTokenIfNeeded(): String? {
        requireIsInitialized()
        if (shouldRefreshAccessToken()) {
            refreshAccessToken()
        }
        return accessToken
    }

    @MainThread
    fun addOnSessionStateChangedListener(
        listener: OnSessionStateChangedListener,
        handler: Handler = Handler(
            Looper.getMainLooper()
        )
    ) {
        requireIsMainThread()
        onSessionStateChangedListeners.add(ListenerPair(listener, handler))
    }

    @MainThread
    fun removeOnSessionStateChangedListener(listener: OnSessionStateChangedListener) {
        requireIsMainThread()
        onSessionStateChangedListeners.removeIf { it.listener == listener }
    }

    private fun updateSessionState(state: SessionState, reason: SessionStateChangeReason) {
        // TODO: Add re-entry detection
        sessionState = state
        onSessionStateChangedListeners.forEach {
            it.handler.post {
                it.listener.onSessionStateChanged(authgear, reason)
            }
        }
    }

    private fun JwkResponse.toHeader(): JsonObject {
        val header = mutableMapOf<String, JsonElement>()
        header["typ"] = JsonPrimitive("vnd.authgear.anonymous-request")
        header["kid"] = JsonPrimitive(kid)
        header["alg"] = JsonPrimitive(alg)
        jwk?.let { jwk ->
            header["jwk"] = JsonObject(mutableMapOf<String, JsonElement>().also {
                it["kid"] = JsonPrimitive(jwk.kid)
                it["kty"] = JsonPrimitive(jwk.kty)
                it["n"] = JsonPrimitive(jwk.n)
                it["e"] = JsonPrimitive(jwk.e)
            })
        }
        return JsonObject(header)
    }

    private fun getJwt(
        kid: String,
        header: Map<String, JsonElement>,
        payload: Map<String, String>
    ): String {
        val jwtData = prepareJwtData(header, payload)
        val sig = keyRepo.signAnonymousToken(kid, jwtData)
        return "$jwtData.$sig"
    }

    private fun authorizeEndpoint(options: AuthorizeOptions): String {
        val config = oauthRepo.getOIDCConfiguration()
        val queries = mutableMapOf<String, String>()
        val codeVerifier = setupVerifier()
        tokenRepo.setOIDCCodeVerifier(name, codeVerifier.verifier)
        queries["response_type"] = "code"
        queries["scope"] = "openid offline_access https://authgear.com/scopes/full-access"
        queries["code_challenge_method"] = "S256"
        queries["code_challenge"] = codeVerifier.challenge
        queries["client_id"] = clientId
        queries["redirect_uri"] = options.redirectUri
        options.state?.let {
            queries["state"] = it
        }
        options.prompt?.let {
            queries["prompt"] = it
        }
        options.loginHint?.let {
            queries["login_hint"] = it
        }
        options.uiLocales?.let {
            queries["ui_locales"] = it.joinToString(separator = " ")
        }
        return "${config.authorizationEndpoint}?${queries.toQueryParameter()}"
    }

    private fun setupVerifier(): Verifier {
        val verifier = generateCodeVerifier()
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
        val refreshToken = tokenRepo.getRefreshToken(name)
        if (refreshToken == null) {
            // Somehow we are asked to refresh access token but we don't have the refresh token.
            // Something went wrong, clear session.
            clearSession(SessionStateChangeReason.NoToken)
            return
        }
        val tokenResponse: OIDCTokenResponse?
        try {
            tokenResponse = oauthRepo.oidcTokenRequest(
                OIDCTokenRequest(
                    grantType = GrantType.REFRESH_TOKEN,
                    clientId = clientId,
                    refreshToken = refreshToken
                )
            )
        } catch (e: Exception) {
            if (e is OauthException && e.error == "invalid_grant") {
                clearSession(SessionStateChangeReason.Expired)
                return
            }
            throw e
        }
        saveToken(tokenResponse, SessionStateChangeReason.FoundToken)
    }

    private fun saveToken(tokenResponse: OIDCTokenResponse, reason: SessionStateChangeReason) {
        synchronized(this) {
            accessToken = tokenResponse.accessToken
            refreshToken = tokenResponse.refreshToken
            expireAt =
                Instant.now() + Duration.ofMillis((tokenResponse.expiresIn * ExpireInPercentage).toLong())
            updateSessionState(SessionState.LoggedIn, reason)
        }
        val refreshToken = this.refreshToken
        if (refreshToken != null) {
            tokenRepo.setRefreshToken(name, refreshToken)
        }
    }

    private fun clearSession(changeReason: SessionStateChangeReason) {
        tokenRepo.deleteRefreshToken(name)
        synchronized(this) {
            accessToken = null
            refreshToken = null
            expireAt = null
            updateSessionState(SessionState.NoSession, changeReason)
        }
    }

    private suspend fun openAuthorizeUrl(redirectUrl: String, authorizeUrl: String): String {
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
        val error = uri.getQueryParameter("error")
        if (error != null) {
            throw OauthException(error, uri.getQueryParameter("error_description") ?: "")
        }
        val code = uri.getQueryParameter("code")
            ?: throw OauthException("invalid_request", "Missing parameter: code")
        val codeVerifier = tokenRepo.getOIDCCodeVerifier(name)
        val tokenResponse = oauthRepo.oidcTokenRequest(
            OIDCTokenRequest(
                grantType = GrantType.AUTHORIZATION_CODE,
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = codeVerifier ?: ""
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(tokenResponse.accessToken)
        saveToken(tokenResponse, SessionStateChangeReason.Authorized)
        return AuthorizeResult(userInfo, uri.getQueryParameter("state"))
    }
}
