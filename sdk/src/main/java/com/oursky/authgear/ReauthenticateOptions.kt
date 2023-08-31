package com.oursky.authgear

import com.oursky.authgear.oauth.OidcAuthenticationRequest

/**
 * Reauthentication options.
 */
data class ReauthenticateOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    var redirectUri: String,
    /**
     * OAuth 2.0 state value.
     */
    var state: String? = null,
    /**
     * Custom state.
     */
    var xState: String? = null,
    /**
     * UI locale tags
     */
    var uiLocales: List<String>? = null,
    /**
     * Theme override
     */
    var colorScheme: ColorScheme? = null,

    /**
     * OIDC max_age.
     * The default is 0 if not provided.
     */
    var maxAge: Int? = null,

    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The wechatRedirectURI will be called when user click the login with WeChat button
     */
    var wechatRedirectURI: String? = null
)

internal fun ReauthenticateOptions.toRequest(idTokenHint: String, isSsoEnabled: Boolean): OidcAuthenticationRequest {
    return OidcAuthenticationRequest(
        redirectUri = this.redirectUri,
        responseType = "code",
        scope = listOf("openid", "https://authgear.com/scopes/full-access"),
        isSsoEnabled = isSsoEnabled,
        state = this.state,
        xState = this.xState,
        prompt = null,
        loginHint = null,
        idTokenHint = idTokenHint,
        maxAge = this.maxAge ?: 0,
        uiLocales = this.uiLocales,
        colorScheme = this.colorScheme,
        wechatRedirectURI = this.wechatRedirectURI,
        page = null
    )
}
