package com.oursky.authgear

data class PreAuthenticatedURLOptions @JvmOverloads constructor(
    /**
     * The client ID of the web application.
     */
    var webApplicationClientID: String,
    /**
     * The URI the browser should go to after successfully obtained a authenticated session.
     */
    var webApplicationURI: String,
    /**
     * Any string that will be passed to redirectURI by the `state` query parameter.
     */
    var state: String?,
    /**
     * Resource indicator (RFC 8707) of the new session. Defaults to the `resource` given to `configure`; a value given here replaces it.
     */
    var resource: String? = null
)