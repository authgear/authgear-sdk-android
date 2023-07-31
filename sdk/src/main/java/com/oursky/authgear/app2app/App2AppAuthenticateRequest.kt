package com.oursky.authgear.app2app

import android.net.Uri
import com.oursky.authgear.net.toQueryParameter

internal data class App2AppAuthenticateRequest(
    var authorizationEndpoint: String,
    var redirectUri: String,
    var clientID: String,
    var codeChallenge: String
)

internal fun App2AppAuthenticateRequest.toUri(): Uri {
    val query: Map<String, String> = hashMapOf(
        "client_id" to clientID,
        "redirect_uri" to this.redirectUri,
        "code_challenge_method" to "S256",
        "code_challenge" to codeChallenge
    )
    return Uri.parse(this.authorizationEndpoint).buildUpon()
        .encodedQuery(query.toQueryParameter())
        .build()
}