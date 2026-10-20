package com.oursky.authgear

data class PromoteOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after authorization.
     */
    var redirectUri: String,
    /**
     * Use this parameter to provide parameters from the client application to Custom UI. The string in xState can be accessed by the Custom UI. Ignore this parameter if default AuthUI is used
     */
    var xState: String? = null,
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
     * Resource indicator (RFC 8707). Defaults to the `resource` given to `configure`; a value given here replaces it.
     *
     * The access tokens of the resulting session have `aud` set to this URI.
     */
    var resource: String? = null,

    /**
     * Scopes of the API Resource given in [resource] to request. Defaults to the `scopes` given to `configure`; a value given here replaces it.
     *
     * They are merged with the scopes required by the SDK.
     */
    var scopes: List<String>? = null
)
