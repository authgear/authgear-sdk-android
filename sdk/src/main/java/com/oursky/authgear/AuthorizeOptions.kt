package com.oursky.authgear

/**
 * Authorization options.
 */
data class AuthorizeOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    var redirectUri: String,
    /**
     * OAuth 2.0 state value.
     */
    var state: String? = null,
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
     * WeChat redirect uri is needed when integrating WeChat login
     * The wechatRedirectURI will be called when user click the login with WeChat button
     */
    var wechatRedirectURI: String? = null,

    /**
     * Which page to open initially. Valid values are "login" and "signup".
     */
    var page: String? = null
)
