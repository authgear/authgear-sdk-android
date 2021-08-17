package com.oursky.authgear.oauth

import com.oursky.authgear.AuthgearCore
import com.oursky.authgear.PromptOption

internal data class OIDCAuthenticationRequest constructor(
    var redirectUri: String,
    var responseType: String,
    var scope: List<String>,
    var state: String? = null,
    var prompt: List<PromptOption>? = null,
    var maxAge: Int? = null,
    var loginHint: String? = null,
    var uiLocales: List<String>? = null,
    var idTokenHint: String? = null,
    var wechatRedirectURI: String? = null,
    var page: String? = null,
    var suppressIDPSessionCookie: Boolean? = null
)

internal fun OIDCAuthenticationRequest.toQuery(clientID: String, codeVerifier: AuthgearCore.Verifier?): Map<String, String> {
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
        query["prompt"] = it.joinToString(separator = " ") { it.raw }
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

    if (this.suppressIDPSessionCookie == true) {
        query["x_suppress_idp_session_cookie"] = "true"
    }

    return query
}
