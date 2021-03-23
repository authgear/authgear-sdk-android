package com.oursky.authgear.data.oauth

import com.oursky.authgear.GrantType
import com.oursky.authgear.UserInfo
import com.oursky.authgear.data.HttpClient
import com.oursky.authgear.oauth.*
import java.net.URL

internal class OauthRepoHttp : OauthRepo {
    companion object {
        @Suppress("unused")
        private val TAG = OauthRepoHttp::class.java.simpleName
    }

    private var config: OIDCConfiguration? = null

    // Variable assignment is atomic in kotlin so no need to guard
    // If memory ordering becomes a problem, use AtomicReference (instead of synchronize)
    override var endpoint: String? = null
    override fun getOIDCConfiguration(): OIDCConfiguration {
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
            val newConfig: OIDCConfiguration =
                HttpClient.getJson(URL(URL(endpoint), "/.well-known/openid-configuration"))
            this.config = newConfig
            return newConfig
        }
    }

    override fun oidcTokenRequest(request: OIDCTokenRequest): OIDCTokenResponse {
        val config = getOIDCConfiguration()
        val body = mutableMapOf<String, String>()
        body["grant_type"] = request.grantType.raw
        body["client_id"] = request.clientId
        request.redirectUri?.let { body["redirect_uri"] = it }
        request.code?.let { body["code"] = it }
        request.codeVerifier?.let { body["code_verifier"] = it }
        request.refreshToken?.let { body["refresh_token"] = it }
        request.jwt?.let { body["jwt"] = it }
        return HttpClient.postFormRespJsonWithError<OIDCTokenResponse, OauthException>(
            URL(config.tokenEndpoint),
            body
        )
    }

    override fun biometricSetupRequest(accessToken: String, clientId: String, jwt: String) {
        val config = getOIDCConfiguration()
        val body = mutableMapOf<String, String>()
        body["client_id"] = clientId
        body["grant_type"] = GrantType.BIOMETRIC.raw
        body["jwt"] = jwt
        return HttpClient.postForm(
            URL(config.tokenEndpoint),
            body,
            mutableMapOf(
                Pair("authorization", "Bearer $accessToken")
            )
        )
    }

    override fun oidcRevocationRequest(refreshToken: String) {
        val config = getOIDCConfiguration()
        val queries = mutableMapOf<String, String>()
        queries["token"] = refreshToken
        HttpClient.postForm(URL(config.revocationEndpoint), queries, null)
    }

    override fun oidcUserInfoRequest(accessToken: String): UserInfo {
        val config = getOIDCConfiguration()
        return HttpClient.getJson(
            URL(config.userInfoEndpoint),
            mutableMapOf(Pair("authorization", "Bearer $accessToken"))
        )
    }

    override fun oauthChallenge(purpose: String): ChallengeResponse {
        val body = mutableMapOf<String, String>()
        body["purpose"] = purpose
        val response: ChallengeResponseResult =
            HttpClient.postJsonRespJson(URL(URL(endpoint), "/oauth2/challenge"), body)
        return response.result
    }

    override fun oauthAppSessionToken(refreshToken: String): AppSessionTokenResponse {
        val body = mutableMapOf<String, String>()
        body["refresh_token"] = refreshToken
        val response: AppSessionTokenResponseResult =
            HttpClient.postJsonRespJson(URL(URL(endpoint), "/oauth2/app_session_token"), body)
        return response.result
    }

    override fun weChatAuthCallback(code: String, state: String) {
        val body = mutableMapOf<String, String>()
        body["code"] = code
        body["state"] = state
        body["x_platform"] = "android"
        HttpClient.postForm(URL(URL(endpoint), "/sso/wechat/callback"), body, null)
    }
}
