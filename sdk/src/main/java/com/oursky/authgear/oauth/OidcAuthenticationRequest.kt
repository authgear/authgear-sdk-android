package com.oursky.authgear.oauth

import com.oursky.authgear.AuthgearCore
import com.oursky.authgear.ColorScheme
import com.oursky.authgear.PromptOption
import com.oursky.authgear.SettingsAction

internal data class OidcAuthenticationRequest constructor(
    var redirectUri: String,
    var responseType: String,
    var scope: List<String>,
    var isSsoEnabled: Boolean,
    var state: String? = null,
    var prompt: List<PromptOption>? = null,
    var maxAge: Int? = null,
    var loginHint: String? = null,
    var uiLocales: List<String>? = null,
    var colorScheme: ColorScheme? = null,
    var idTokenHint: String? = null,
    var wechatRedirectURI: String? = null,
    var page: String? = null,
    var customUIQuery: String? = null,
    var settingsAction: SettingsAction? = null
)

internal fun OidcAuthenticationRequest.toQuery(clientID: String, codeVerifier: AuthgearCore.Verifier?): Map<String, String> {
    val query = mutableMapOf(
        "client_id" to clientID,
        "response_type" to this.responseType,
        "redirect_uri" to this.redirectUri,
        "scope" to this.scope.joinToString(separator = " "),
        "x_platform" to "android"
    )

    codeVerifier?.let {
        query["code_challenge_method"] = "S256"
        query["code_challenge"] = it.challenge
    }

    this.prompt?.let {
        query["prompt"] = it.joinToString(separator = " ") { p -> p.raw }
    }

    this.state?.let {
        query["state"] = it
    }

    this.loginHint?.let {
        query["login_hint"] = it
    }

    this.uiLocales?.let {
        query["ui_locales"] = it.joinToString(separator = " ")
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

    this.customUIQuery?.let {
        query["x_custom_ui_query"] = it
    }

    this.settingsAction?.let {
        query["x_settings_action"] = it.raw
    }

    if (!this.isSsoEnabled) {
        // For backward compatibility
        // If the developer updates the SDK but not the server
        query["x_suppress_idp_session_cookie"] = "true"
    }

    query["x_sso_enabled"] = if (this.isSsoEnabled) "true" else "false"

    return query
}
