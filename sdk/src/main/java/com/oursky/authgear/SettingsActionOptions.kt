package com.oursky.authgear

import com.oursky.authgear.oauth.OidcAuthenticationRequest

data class SettingsActionOptions @JvmOverloads constructor(
    var redirectURI: String,
    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The wechatRedirectURI will be called when user click the login with WeChat button
     */
    var wechatRedirectURI: String? = null,
    /**
     * Theme override
     */
    var colorScheme: ColorScheme? = null,
    var uiLocales: List<String>? = null,
)

internal fun SettingsActionOptions.toRequest(action: SettingsAction, idTokenHint: String, loginHint: String): OidcAuthenticationRequest {
    return OidcAuthenticationRequest(
        redirectUri = this.redirectURI,
        responseType = "urn:authgear:params:oauth:response-type:settings-action",
        // offline_access is not needed because we don't want a new refresh token to be generated
        // device_sso and app-initiated-sso-to-web is also not needed,
        // because session for settings should not be used to perform SSO.
        scope = listOf("openid", "https://authgear.com/scopes/full-access"),
        isSsoEnabled = false,
        prompt = listOf(PromptOption.NONE),
        loginHint = loginHint,
        idTokenHint = idTokenHint,
        uiLocales = this.uiLocales,
        colorScheme = this.colorScheme,
        wechatRedirectURI = this.wechatRedirectURI,
        page = null,
        settingsAction = action.raw,
    )
}
