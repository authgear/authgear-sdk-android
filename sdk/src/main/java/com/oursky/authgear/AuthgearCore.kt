package com.oursky.authgear

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.util.Base64
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
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Listeners should only be set on the main thread.
 */
internal class AuthgearCore(
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
    var onRefreshTokenExpiredListener: ListenerPair<OnRefreshTokenExpiredListener>? = null
        set(value) {
            requireIsMainThread()
            field = value
        }
    var sessionState: SessionState = SessionState.Unknown
        private set
    private val refreshAccessTokenJob = AtomicReference<Job>(null)

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
                grantType = "urn:authgear:params:oauth:grant-type:anonymous-request",
                clientId = clientId,
                jwt = jwt
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(
            tokenResponse.accessToken
        )
        saveToken(tokenResponse)
        tokenRepo.setAnonymousKeyId(name, key.kid)
        return userInfo
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun authorize(options: AuthorizeOptions): String? {
        requireIsInitialized()
        val authorizeUrl = authorizeEndpoint(options)
        val deepLink = openAuthorizeUrl(options.redirectUri, authorizeUrl)
        return finishAuthorization(deepLink).state
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun configure(skipRefreshAccessToken: Boolean = false) {
        // TODO: This is not present in js sdk. Verify if this is needed.
        if (isInitialized) return
        isInitialized = true
        val refreshToken = tokenRepo.getRefreshToken(name)
        this.refreshToken = refreshToken
        if (shouldRefreshAccessToken()) {
            if (skipRefreshAccessToken) {
                // Consider user as logged in if refresh token is available
                sessionState = SessionState.LoggedIn
            } else {
                refreshAccessToken()
            }
        } else {
            sessionState = SessionState.NoSession
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
        clearSession()
    }

    fun openUrl(path: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(URL(URL(authgearEndpoint), path).toString())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    fun open(page: Page) {
        openUrl(
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
        queries["redirect_url"] = options.redirectUri
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
        // TODO: This would lead to 401 since the token might be expired after this is ran. Add threshold?
        // NOTE: The threshold is *NOT* present in js sdk
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
            clearSession()
            return
        }
        val tokenResponse: OIDCTokenResponse?
        try {
            tokenResponse = oauthRepo.oidcTokenRequest(
                OIDCTokenRequest(
                    grantType = "refresh_token",
                    clientId = clientId,
                    refreshToken = refreshToken
                )
            )
        } catch (e: Exception) {
            if (e is OauthException && e.error == "invalid_grant") {
                onRefreshTokenExpiredListener?.let {
                    it.handler.post { it.listener.onExpired() }
                }
                clearSession()
                return
            }
            throw e
        }
        saveToken(tokenResponse)
    }

    private fun saveToken(tokenResponse: OIDCTokenResponse) {
        accessToken = tokenResponse.accessToken
        sessionState = SessionState.LoggedIn
        val refreshToken = tokenResponse.refreshToken
        this.refreshToken = refreshToken
        expireAt = Instant.now() + Duration.ofMillis(tokenResponse.expiresIn)
        if (refreshToken != null) {
            tokenRepo.setRefreshToken(name, refreshToken)
        }
    }

    private fun clearSession() {
        tokenRepo.deleteRefreshToken(name)
        accessToken = null
        refreshToken = null
        expireAt = null
        sessionState = SessionState.NoSession
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
                grantType = "authorization_code",
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = codeVerifier ?: ""
            )
        )
        val userInfo = oauthRepo.oidcUserInfoRequest(tokenResponse.accessToken)
        saveToken(tokenResponse)
        return AuthorizeResult(userInfo, uri.getQueryParameter("state"))
    }
}
