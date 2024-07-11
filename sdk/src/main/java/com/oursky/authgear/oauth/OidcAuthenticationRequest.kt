package com.oursky.authgear.oauth

import com.oursky.authgear.AuthgearCore
import com.oursky.authgear.ColorScheme
import com.oursky.authgear.PromptOption
import com.oursky.authgear.UILocales

internal data class OidcAuthenticationRequest constructor(
    var redirectUri: String,
    var responseType: String,
    var scope: List<String>? = null,
    var isSsoEnabled: Boolean? = null,
    var state: String? = null,
    var xState: String? = null,
    var prompt: List<PromptOption>? = null,
    var maxAge: Int? = null,
    var loginHint: String? = null,
    var uiLocales: List<String>? = null,
    var colorScheme: ColorScheme? = null,
    var idTokenHint: String? = null,
    var wechatRedirectURI: String? = null,
    var page: String? = null,
    var settingsAction: String? = null,
    var authenticationFlowGroup: String? = null,
    var responseMode: String? = null,
    var xPreAuthenticatedURLToken: String? = null
)

internal fun OidcAuthenticationRequest.toQuery(clientID: String, codeVerifier: AuthgearCore.Verifier?): Map<String, String> {
    val query = mutableMapOf(
        "client_id" to clientID,
        "response_type" to this.responseType,
        "redirect_uri" to this.redirectUri,
        "x_platform" to "android"
    )

    codeVerifier?.let {
        query["code_challenge_method"] = AuthgearCore.CODE_CHALLENGE_METHOD
        query["code_challenge"] = it.challenge
    }

    this.scope?.let {
        query["scope"] = it.joinToString(separator = " ")
    }

    this.prompt?.let {
        query["prompt"] = it.joinToString(separator = " ") { p -> p.raw }
    }

    this.state?.let {
        query["state"] = it
    }

    this.xState?.let {
        query["x_state"] = it
    }

    this.loginHint?.let {
        query["login_hint"] = it
    }

    this.uiLocales?.let {
        query["ui_locales"] = UILocales.stringify(it)
    }

    this.colorScheme?.let {
        query["x_color_scheme"] = it.raw
    }

    this.idTokenHint?.let {
        query["id_token_hint"] = it
    }

    this.maxAge?.let {
        query["max_age"] = it.toString()
    }

    this.wechatRedirectURI?.let {
        query["x_wechat_redirect_uri"] = it
    }

    this.page?.let {
        query["x_page"] = it
    }

    this.settingsAction?.let {
        query["x_settings_action"] = it
    }

    this.responseMode?.let {
        query["response_mode"] = it
    }

    this.xPreAuthenticatedURLToken?.let {
        query["x_pre_authenticated_url_token"] = it
    }

    val isSsoEnabled = this.isSsoEnabled ?: false

    if (!isSsoEnabled) {
        // For backward compatibility
        // If the developer updates the SDK but not the server
        query["x_suppress_idp_session_cookie"] = "true"
    }

    query["x_sso_enabled"] = if (isSsoEnabled) "true" else "false"

    this.authenticationFlowGroup?.let {
        query["x_authentication_flow_group"] = it
    }

    return query
}
