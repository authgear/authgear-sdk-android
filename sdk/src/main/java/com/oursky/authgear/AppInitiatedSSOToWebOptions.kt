package com.oursky.authgear

data class PreAuthenticatedURLOptions @JvmOverloads constructor(
    /**
     * The client ID of the new authenticated session in web.
     */
    var clientID: String,
    /**
     * The URI the browser should go to after successfully obtained a authenticated session.
     */
    var redirectURI: String,
    /**
     * Any string that will be passed to redirectURI by the `state` query parameter.
     */
    var state: String?
)