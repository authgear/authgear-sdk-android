package com.oursky.authgear.app2app

import com.oursky.authgear.AuthgearCore


data class App2AppAuthenticateOptions @JvmOverloads constructor(
    var authorizationEndpoint: String,
    var redirectUri: String,
    var state: String? = null,
    /**
     * Resource indicator (RFC 8707). Defaults to the `resource` given to `configure`; a value given here replaces it.
     * The authenticated app passes it on unchanged.
     */
    var resource: String? = null
)

internal fun App2AppAuthenticateOptions.toRequest(
    clientID: String,
    codeVerifier: AuthgearCore.Verifier
): App2AppAuthenticateRequest {
    return App2AppAuthenticateRequest(
        authorizationEndpoint = authorizationEndpoint,
        redirectUri = redirectUri,
        clientID = clientID,
        codeChallenge = codeVerifier.challenge,
        state = state,
        resource = resource,
    )
}
