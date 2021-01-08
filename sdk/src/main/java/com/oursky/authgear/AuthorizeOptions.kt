package com.oursky.authgear

/**
 * Authorization options.
 */
data class AuthorizeOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    val redirectUri: String,
    /**
     * OAuth 2.0 state value.
     */
    val state: String? = null,
    /**
     * OIDC response type parameter.
     */
    val responseType: String? = "code",
    /**
     * OIDC prompt parameter. Default value is `login` so that re-login after logout does not obtain
     * the refresh token automatically if the cookies in the external browser persist.
     *
     * It is recommended that you do not modify this parameter unless you are familiar with the OIDC
     * spec.
     */
    val prompt: String? = "login",
    /**
     * OIDC login hint parameter
     */
    val loginHint: String? = null,
    /**
     * UI locale tags
     */
    val uiLocales: List<String>? = null,
    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The weChatRedirectURI will be called when user click the login with WeChat button
     */
    val weChatRedirectURI: String? = null
)
