package com.oursky.authgear.data.oauth

import android.net.Uri
import com.oursky.authgear.AuthgearException
import com.oursky.authgear.GrantType
import com.oursky.authgear.UserInfo
import com.oursky.authgear.net.HTTPClientHelper
import com.oursky.authgear.dpop.DPoPProvider
import com.oursky.authgear.getOrigin
import com.oursky.authgear.net.HTTPClient
import com.oursky.authgear.net.HTTPRequest
import com.oursky.authgear.net.HTTPResponse
import com.oursky.authgear.net.toFormData
import com.oursky.authgear.oauth.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.charset.Charset

internal class OAuthRepoHttp(
    private val httpClient: HTTPClient,
    private val dPoPProvider: DPoPProvider
) : OAuthRepo {
    companion object {
        @Suppress("unused")
        private val TAG = OAuthRepoHttp::class.java.simpleName

        private val UTF_8 = Charset.forName("UTF-8")
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
            val response = fetchWithDPoP(
                uri = URL(URL(endpoint), "/.well-known/openid-configuration").toURI(),
                method = "GET",
                headers = mutableMapOf(),
                requestBodyBytes = null,
            )
            val responseString = response.body.use {
                String(it.readBytes(), UTF_8)
            }
            HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
            val newConfig: OidcConfiguration = HTTPClientHelper.json.decodeFromString(responseString)
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
            "content-type" to mutableListOf("application/x-www-form-urlencoded")
        )
        request.accessToken?.let {
            headers["authorization"] = mutableListOf("Bearer $it")
        }

        val response = fetchWithDPoP(
            uri = URI(config.tokenEndpoint),
            method = "POST",
            headers = headers,
            requestBodyBytes = body.toFormData().toByteArray(UTF_8)
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
        val responseBody: OidcTokenResponse = HTTPClientHelper.json.decodeFromString(responseString)
        return responseBody
    }

    override fun biometricSetupRequest(accessToken: String, clientId: String, jwt: String) {
        val config = getOidcConfiguration()
        val body = mutableMapOf<String, String>()
        body["client_id"] = clientId
        body["grant_type"] = GrantType.BIOMETRIC.raw
        body["jwt"] = jwt

        val response = fetchWithDPoP(
            uri = URI(config.tokenEndpoint),
            method = "POST",
            headers = mutableMapOf(
                "authorization" to mutableListOf("Bearer $accessToken"),
                "content-type" to mutableListOf("application/x-www-form-urlencoded"),
            ),
            requestBodyBytes = body.toFormData().toByteArray(UTF_8),
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
    }

    override fun oidcRevocationRequest(refreshToken: String) {
        val config = getOidcConfiguration()
        val body = mutableMapOf<String, String>()
        body["token"] = refreshToken

        val response = fetchWithDPoP(
            uri = URI(config.revocationEndpoint),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to mutableListOf("application/x-www-form-urlencoded")
            ),
            requestBodyBytes = body.toFormData().toByteArray(UTF_8)
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
    }

    override fun oidcUserInfoRequest(accessToken: String): UserInfo {
        val config = getOidcConfiguration()
        val response = fetchWithDPoP(
            uri = URI(config.userInfoEndpoint),
            method = "GET",
            headers = mutableMapOf(
                "authorization" to mutableListOf("Bearer $accessToken")
            ),
            requestBodyBytes = null
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
        val responseBody: UserInfo = HTTPClientHelper.json.decodeFromString(responseString)
        return responseBody
    }

    override fun oauthChallenge(purpose: String): ChallengeResponse {
        val body = mutableMapOf<String, String>()
        body["purpose"] = purpose
        val response = fetchWithDPoP(
            uri = buildApiUrl("/oauth2/challenge"),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to mutableListOf("application/json")
            ),
            requestBodyBytes = HTTPClientHelper.json.encodeToString(body).toByteArray(UTF_8),
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
        val responseBody: ChallengeResponseResult = HTTPClientHelper.json.decodeFromString(responseString)
        return responseBody.result
    }

    override fun oauthAppSessionToken(refreshToken: String): AppSessionTokenResponse {
        val body = mutableMapOf<String, String>()
        body["refresh_token"] = refreshToken
        val response = fetchWithDPoP(
            uri = buildApiUrl("/oauth2/app_session_token"),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to mutableListOf("application/json")
            ),
            requestBodyBytes = HTTPClientHelper.json.encodeToString(body).toByteArray(UTF_8)
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
        val responseBody: AppSessionTokenResponseResult = HTTPClientHelper.json.decodeFromString(responseString)
        return responseBody.result
    }

    override fun wechatAuthCallback(code: String, state: String) {
        val body = mutableMapOf<String, String>()
        body["code"] = code
        body["state"] = state
        body["x_platform"] = "android"
        val response = fetchWithDPoP(
            uri = buildApiUrl("/sso/wechat/callback"),
            method = "POST",
            headers = mutableMapOf(
                "content-type" to mutableListOf("application/x-www-form-urlencoded")
            ),
            requestBodyBytes = body.toFormData().toByteArray(UTF_8),
        )
        val responseString = response.body.use {
            String(it.readBytes(), UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
    }

    private fun fetchWithDPoP(
        uri: URI,
        method: String,
        headers: MutableMap<String, MutableList<String>>,
        requestBodyBytes: ByteArray?
    ): HTTPResponse {
        val dpopProof = dPoPProvider.generateDPoPProof(
            htm = method,
            htu = uri.toString()
        )
        dpopProof?.let {
            headers["DPoP"] = mutableListOf(dpopProof)
        }

        var requestBody: InputStream? = null
        if (requestBodyBytes != null) {
            requestBody = ByteArrayInputStream(requestBodyBytes)
        }

        val response = this.httpClient.send(HTTPRequest(
            method = method,
            headers = headers,
            uri = uri,
            body = requestBody,
        ))

        return response
    }

    private fun buildApiUrl(path: String): URI {
        val config = getOidcConfiguration()
        val builder = Uri.parse(config.authorizationEndpoint).getOrigin()?.let {
            Uri.parse(it).buildUpon()
        } ?: throw AuthgearException("invalid authorization_endpoint")
        builder.path(path)
        return URI(builder.build().toString())
    }
}
