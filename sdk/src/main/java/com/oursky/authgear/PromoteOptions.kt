package com.oursky.authgear

data class PromoteOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    val redirectUri: String,
    /**
     * OAuth 2.0 state value.
     */
    val state: String? = null,
    /**
     * UI locale tags
     */
    val uiLocales: List<String>? = null,
    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The weChatRedirectURI will be called when user click the login with WeChat button
     */
    var weChatRedirectURI: String? = null
)
