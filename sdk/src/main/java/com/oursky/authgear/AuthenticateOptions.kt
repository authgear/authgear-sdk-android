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
     * OAuth 2.0 state value.
     */
    var state: String? = null,
    /**
     * Custom state.
     */
    var xState: String? = null,
    /**
     * OIDC response type parameter.
     */
    var responseType: String? = "code",
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
     * OIDC login hint parameter
     */
    var loginHint: String? = null,
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
    var authenticationFlowGroup: String? = null
) {
    companion object {
        internal fun getScopes(isAppInitiatedSSOToWebEnabled: Boolean): List<String> {
            val scopes = mutableListOf("openid", "offline_access", "https://authgear.com/scopes/full-access")
            if (isAppInitiatedSSOToWebEnabled) {
                scopes.add("device_sso")
                scopes.add("https://authgear.com/scopes/pre-authenticated-url")
            }
            return scopes
        }
    }
}

internal fun AuthenticateOptions.toRequest(
    isSsoEnabled: Boolean,
    isAppInitiatedSSOToWebEnabled: Boolean
): OidcAuthenticationRequest {
    return OidcAuthenticationRequest(
        redirectUri = this.redirectUri,
        responseType = "code",
        scope = AuthenticateOptions.getScopes(isAppInitiatedSSOToWebEnabled),
        isSsoEnabled = isSsoEnabled,
        state = this.state,
        xState = this.xState,
        prompt = this.prompt,
        loginHint = this.loginHint,
        idTokenHint = null,
        maxAge = null,
        uiLocales = this.uiLocales,
        colorScheme = this.colorScheme,
        wechatRedirectURI = this.wechatRedirectURI,
        page = this.page,
        authenticationFlowGroup = this.authenticationFlowGroup
    )
}