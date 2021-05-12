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
     * OIDC prompt parameter. Default value is `login` so that re-login after logout does not obtain
     * the refresh token automatically if the cookies in the external browser persist.
     *
     * It is recommended that you do not modify this parameter unless you are familiar with the OIDC
     * spec.
     */
    var prompt: String? = null,
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
