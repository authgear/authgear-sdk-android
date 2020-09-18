package com.oursky.authgear

data class AuthorizeOptions(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    val redirectUri: String,
    /**
     * OAuth 2.0 state value.
     */
    val state: String? = null,
    /**
     * OIDC prompt parameter.
     */
    val prompt: String? = null,
    /**
     * OIDC login hint parameter
     */
    val loginHint: String? = null,
    /**
     * UI locale tags
     */
    val uiLocales: List<String>? = null
)
