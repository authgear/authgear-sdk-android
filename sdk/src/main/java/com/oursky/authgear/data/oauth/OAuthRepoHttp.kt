package com.oursky.authgear.data.oauth

import android.net.Uri
import com.oursky.authgear.AuthgearException
import com.oursky.authgear.GrantType
import com.oursky.authgear.UserInfo
import com.oursky.authgear.data.HttpClient
import com.oursky.authgear.dpop.DPoPProvider
import com.oursky.authgear.getOrigin
import com.oursky.authgear.net.toFormData
import com.oursky.authgear.oauth.*
import kotlinx.serialization.encodeToString
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

internal class OAuthRepoHttp(
    private val dPoPProvider: DPoPProvider
) : OAuthRepo {
    companion object {
        @Suppress("unused")
        private val TAG = OAuthRepoHttp::class.java.simpleName
    }

    private var config: OidcConfiguration? = null

    // Variable assignment is atomic in kotlin so no need to guard
    // If memory ordering becomes a problem, use AtomicReference (instead of synchronize)
    override var endpoint: String? = null
    override fun getOidcConfiguration(): OidcConfiguration {
        require(endpoint != null) {
            "Missing endpoint in oauth repository"
        }
        val endpoint = this.endpoint
        val config = this.config
        if (config != null) return config
        // Double-checked locking
        synchronized(this) {
            val configAfterAcquire = this.config
            if (configAfterAcquire != null) return configAfterAcquire
            val url = URL(URL(endpoint), "/.well-known/openid-configuration")
            val newConfig: OidcConfiguration = fetchWithDPoP(url = url, method = "GET", headers = emptyMap()) { conn ->
                conn.errorStream?.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                }
                conn.inputStream.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                    HttpClient.json.decodeFromString(responseString)
                }
            }
            this.config = newConfig
            return newConfig
        }
    }

    override fun oidcTokenRequest(request: OidcTokenRequest): OidcTokenResponse {
        val config = getOidcConfiguration()
        val body = mutableMapOf<String, String>()
        body["grant_type"] = request.grantType.raw
        body["client_id"] = request.clientId
        request.xDeviceInfo?.let { body["x_device_info"] = it }
        request.redirectUri?.let { body["redirect_uri"] = it }
        request.code?.let { body["code"] = it }
        request.codeVerifier?.let { body["code_verifier"] = it }
        request.refreshToken?.let { body["refresh_token"] = it }
        request.jwt?.let { body["jwt"] = it }
        request.xApp2AppDeviceKeyJwt?.let { body["x_app2app_device_key_jwt"] = it }
        request.codeChallenge?.let { body["code_challenge"] = it }
        request.codeChallengeMethod?.let { body["code_challenge_method"] = it }
        request.deviceSecret?.let { body["device_secret"] = it }
        request.requestedTokenType?.let { body["requested_token_type"] = it.raw }
        request.audience?.let { body["audience"] = it }
        request.subjectTokenType?.let { body["subject_token_type"] = it.raw }
        request.subjectToken?.let { body["subject_token"] = it }
        request.actorTokenType?.let { body["actor_token_type"] = it.raw }
        request.actorToken?.let { body["actor_token"] = it }
        request.scope?.let { body["scope"] = it.joinToString(" ") }
        val headers = mutableMapOf(
            "content-type" to "application/x-www-form-urlencoded"
        )
        request.accessToken?.let {
            headers["authorization"] = "Bearer $it"
        }
        val url = URL(config.tokenEndpoint)
        val method = "POST"
        val dpopProof = dPoPProvider.generateDPoPProof(
            htm = method,
            htu = url.toString()
        )
        dpopProof?.let {
            headers["DPoP"] = dpopProof
        }
        return fetchWithDPoP(
            url = url,
            method = method,
            headers = headers
        ) { conn ->
            conn.outputStream.use {
                it.write(body.toFormData().toByteArray(StandardCharsets.UTF_8))
            }
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
                HttpClient.json.decodeFromString(responseString)
            }
        }
    }

    override fun biometricSetupRequest(accessToken: String, clientId: String, jwt: String) {
        val config = getOidcConfiguration()
        val body = mutableMapOf<String, String>()
        body["client_id"] = clientId
        body["grant_type"] = GrantType.BIOMETRIC.raw
        body["jwt"] = jwt
        return fetchWithDPoP(
            url = URL(config.tokenEndpoint),
            method = "POST",
            headers = mutableMapOf(
                "authorization" to "Bearer $accessToken",
                "content-type" to "application/x-www-form-urlencoded"
            )
        ) { conn ->
            conn.outputStream.use {
                it.write(body.toFormData().toByteArray(StandardCharsets.UTF_8))
            }
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
        }
    }

    override fun oidcRevocationRequest(refreshToken: String) {
        val config = getOidcConfiguration()
        val body = mutableMapOf<String, String>()
        body["token"] = refreshToken
        fetchWithDPoP(
            url = URL(config.revocationEndpoint),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to "application/x-www-form-urlencoded"
            )
        ) { conn ->
            conn.outputStream.use {
                it.write(body.toFormData().toByteArray(StandardCharsets.UTF_8))
            }
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
        }
    }

    override fun oidcUserInfoRequest(accessToken: String): UserInfo {
        val config = getOidcConfiguration()
        return fetchWithDPoP(
            url = URL(config.userInfoEndpoint),
            method = "GET",
            headers = mutableMapOf(
                "authorization" to "Bearer $accessToken"
            )
        ) { conn ->
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
                HttpClient.json.decodeFromString(responseString)
            }
        }
    }

    override fun oauthChallenge(purpose: String): ChallengeResponse {
        val body = mutableMapOf<String, String>()
        body["purpose"] = purpose
        val response: ChallengeResponseResult = fetchWithDPoP(
            url = buildApiUrl("/oauth2/challenge"),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to "application/json"
            )
        ) { conn ->
            conn.outputStream.use {
                it.write(HttpClient.json.encodeToString(body).toByteArray(StandardCharsets.UTF_8))
            }
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
                HttpClient.json.decodeFromString(responseString)
            }
        }
        return response.result
    }

    override fun oauthAppSessionToken(refreshToken: String): AppSessionTokenResponse {
        val body = mutableMapOf<String, String>()
        body["refresh_token"] = refreshToken
        val response: AppSessionTokenResponseResult = fetchWithDPoP(
            url = buildApiUrl("/oauth2/app_session_token"),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to "application/json"
            )
        ) { conn ->
            conn.outputStream.use {
                it.write(HttpClient.json.encodeToString(body).toByteArray(StandardCharsets.UTF_8))
            }
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
                HttpClient.json.decodeFromString(responseString)
            }
        }
        return response.result
    }

    override fun wechatAuthCallback(code: String, state: String) {
        val body = mutableMapOf<String, String>()
        body["code"] = code
        body["state"] = state
        body["x_platform"] = "android"
        fetchWithDPoP(
            url = buildApiUrl("/sso/wechat/callback"),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to "application/x-www-form-urlencoded"
            )
        ) { conn ->
            conn.outputStream.use {
                it.write(body.toFormData().toByteArray(StandardCharsets.UTF_8))
            }
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
        }
    }

    private fun <T>fetchWithDPoP(
        url: URL,
        method: String,
        headers: Map<String, String>,
        followRedirect: Boolean = true,
        callback: (conn: HttpURLConnection) -> T
    ): T {
        val h = headers.toMutableMap()
        val dpopProof = dPoPProvider.generateDPoPProof(
            htm = method,
            htu = url.toString()
        )
        dpopProof?.let {
            h["DPoP"] = dpopProof
        }
        return HttpClient.fetch(
            url = url,
            method = method,
            headers = h,
            followRedirect = followRedirect,
            callback = callback
        )
    }

    private fun buildApiUrl(path: String): URL {
        val config = getOidcConfiguration()
        val builder = Uri.parse(config.authorizationEndpoint).getOrigin()?.let {
            Uri.parse(it).buildUpon()
        } ?: throw AuthgearException("invalid authorization_endpoint")
        builder.path(path)
        return URL(builder.build().toString())
    }
}
