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
     * Use this parameter to provide parameters from the client application to Custom UI. The string in xState can be accessed by the Custom UI. Ignore this parameter if default AuthUI is used
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
    var wechatRedirectURI: String? = null,

    /**
     * Authentication flow group
     */
    var authenticationFlowGroup: String? = null,

    /**
     * OAuth provider alias
     */
    var oauthProviderAlias: String? = null
)

internal fun ReauthenticateOptions.toRequest(idTokenHint: String, isSsoEnabled: Boolean): OidcAuthenticationRequest {
    return OidcAuthenticationRequest(
        redirectUri = this.redirectUri,
        responseType = "code",
        // offline_access is not needed because we don't want a new refresh token to be generated
        // device_sso and pre-authenticated-url is also not needed,
        // because no new session should be generated so the scopes are not important.
        scope = listOf("openid", "https://authgear.com/scopes/full-access"),
        isSsoEnabled = isSsoEnabled,
        state = null,
        xState = this.xState,
        prompt = null,
        loginHint = null,
        idTokenHint = idTokenHint,
        maxAge = this.maxAge ?: 0,
        uiLocales = this.uiLocales,
        colorScheme = this.colorScheme,
        wechatRedirectURI = this.wechatRedirectURI,
        page = null,
        authenticationFlowGroup = this.authenticationFlowGroup,
        oauthProviderAlias = this.oauthProviderAlias
    )
}
