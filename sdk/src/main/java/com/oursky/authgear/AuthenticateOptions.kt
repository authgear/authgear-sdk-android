package com.oursky.authgear

import com.oursky.authgear.oauth.OidcAuthenticationRequest

/**
 * Authorization options.
 */
data class AuthenticateOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    var redirectUri: String,
    /**
     * Use this parameter to provide parameters from the client application to Custom UI. The string in xState can be accessed by the Custom UI. Ignore this parameter if default AuthUI is used
     */
    var xState: String? = null,
    /**
     * OIDC prompt parameter.
     *
     * Prompt parameter will be used for Authgear authorization, it will also be forwarded to the underlying SSO providers.
     *
     * For Authgear, currently, only login is supported. Other unsupported values will be ignored.
     *
     * For the underlying SSO providers, some providers only support a single value rather than a list.
     * The first supported value will be used for that case.
     * e.g. Azure Active Directory.
     */
    var prompt: List<PromptOption>? = null,
    /**
     * UI locale tags
     */
    var uiLocales: List<String>? = null,
    /**
     * Theme override
     */
    var colorScheme: ColorScheme? = null,
    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The wechatRedirectURI will be called when user click the login with WeChat button
     */
    var wechatRedirectURI: String? = null,

    /**
     * Which page to open initially. Valid values are "login" and "signup".
     */
    var page: String? = null,

    /**
     * Authentication flow group
     */
    var authenticationFlowGroup: String? = null,

    /**
     * OAuth provider alias
     */
    var oauthProviderAlias: String? = null
) {
    companion object {
        internal fun getScopes(preAuthenticatedURLEnabled: Boolean): List<String> {
            val scopes = mutableListOf("openid", "offline_access", "https://authgear.com/scopes/full-access")
            if (preAuthenticatedURLEnabled) {
                scopes.add("device_sso")
                scopes.add("https://authgear.com/scopes/pre-authenticated-url")
            }
            return scopes
        }
    }
}

internal fun AuthenticateOptions.toRequest(
    isSsoEnabled: Boolean,
    preAuthenticatedURLEnabled: Boolean,
    dpopJKT: String?
): OidcAuthenticationRequest {
    return OidcAuthenticationRequest(
        redirectUri = this.redirectUri,
        responseType = "code",
        scope = AuthenticateOptions.getScopes(preAuthenticatedURLEnabled),
        isSsoEnabled = isSsoEnabled,
        state = null,
        xState = this.xState,
        prompt = this.prompt,
        loginHint = null,
        idTokenHint = null,
        maxAge = null,
        uiLocales = this.uiLocales,
        colorScheme = this.colorScheme,
        wechatRedirectURI = this.wechatRedirectURI,
        page = this.page,
        authenticationFlowGroup = this.authenticationFlowGroup,
        dpopJKT = dpopJKT,
        oauthProviderAlias = this.oauthProviderAlias
    )
}