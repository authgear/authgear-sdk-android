package com.oursky.authgear.data.oauth

import com.oursky.authgear.UserInfo
import com.oursky.authgear.oauth.AppSessionTokenResponse
import com.oursky.authgear.oauth.ChallengeResponse
import com.oursky.authgear.oauth.OidcConfiguration
import com.oursky.authgear.oauth.OidcTokenRequest
import com.oursky.authgear.oauth.OidcTokenResponse

/**
 * A thread-safe oauth repository.
 */
internal interface OAuthRepo {
    var endpoint: String?
    fun getOidcConfiguration(): OidcConfiguration
    fun oidcTokenRequest(request: OidcTokenRequest): OidcTokenResponse
    fun biometricSetupRequest(accessToken: String, clientId: String, jwt: String)
    fun oidcRevocationRequest(refreshToken: String)
    fun oidcUserInfoRequest(accessToken: String): UserInfo
    fun oauthChallenge(purpose: String): ChallengeResponse
    fun oauthAppSessionToken(refreshToken: String): AppSessionTokenResponse
    fun wechatAuthCallback(code: String, state: String)
}
