package com.oursky.authgear.data.oauth

import com.oursky.authgear.GrantType
import com.oursky.authgear.UserInfo
import com.oursky.authgear.data.HttpClient
import com.oursky.authgear.net.toFormData
import com.oursky.authgear.oauth.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.net.URL
import java.nio.charset.StandardCharsets

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
            val url = URL(URL(endpoint), "/.well-known/openid-configuration")
            val newConfig: OIDCConfiguration = HttpClient.fetch(url = url, method = "GET", headers = emptyMap()) { conn ->
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

    override fun oidcTokenRequest(request: OIDCTokenRequest): OIDCTokenResponse {
        val config = getOIDCConfiguration()
        val body = mutableMapOf<String, String>()
        body["grant_type"] = request.grantType.raw
        body["client_id"] = request.clientId
        body["x_device_info"] = request.xDeviceInfo
        request.redirectUri?.let { body["redirect_uri"] = it }
        request.code?.let { body["code"] = it }
        request.codeVerifier?.let { body["code_verifier"] = it }
        request.refreshToken?.let { body["refresh_token"] = it }
        request.jwt?.let { body["jwt"] = it }
        val headers = mutableMapOf(
            "content-type" to "application/x-www-form-urlencoded"
        )
        request.accessToken?.let {
            headers["authorization"] = "Bearer $it"
        }
        return HttpClient.fetch(
            url = URL(config.tokenEndpoint),
            method = "POST",
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
        val config = getOIDCConfiguration()
        val body = mutableMapOf<String, String>()
        body["client_id"] = clientId
        body["grant_type"] = GrantType.BIOMETRIC.raw
        body["jwt"] = jwt
        return HttpClient.fetch(
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
        val config = getOIDCConfiguration()
        val body = mutableMapOf<String, String>()
        body["token"] = refreshToken
        HttpClient.fetch(
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
        val config = getOIDCConfiguration()
        return HttpClient.fetch(
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
        val response: ChallengeResponseResult = HttpClient.fetch(
            url = URL(URL(endpoint), "/oauth2/challenge"),
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
        val response: AppSessionTokenResponseResult = HttpClient.fetch(
            url = URL(URL(endpoint), "/oauth2/app_session_token"),
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
        HttpClient.fetch(
            url = URL(URL(endpoint), "/sso/wechat/callback"),
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
}
