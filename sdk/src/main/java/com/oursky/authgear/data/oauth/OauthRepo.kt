package com.oursky.authgear.data.oauth

import com.oursky.authgear.UserInfo
import com.oursky.authgear.oauth.AppSessionTokenResponse
import com.oursky.authgear.oauth.ChallengeResponse
import com.oursky.authgear.oauth.OIDCConfiguration
import com.oursky.authgear.oauth.OIDCTokenRequest
import com.oursky.authgear.oauth.OIDCTokenResponse

/**
 * A thread-safe oauth repository.
 */
internal interface OauthRepo {
    var endpoint: String?
    fun getOIDCConfiguration(): OIDCConfiguration
    fun oidcTokenRequest(request: OIDCTokenRequest): OIDCTokenResponse
    fun oidcRevocationRequest(refreshToken: String)
    fun oidcUserInfoRequest(accessToken: String): UserInfo
    fun oauthChallenge(purpose: String): ChallengeResponse
    fun oauthAppSessionToken(refreshToken: String): AppSessionTokenResponse
}
