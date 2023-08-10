package com.oursky.authgear.app2app

import android.net.Uri
import com.oursky.authgear.AuthgearCore
import com.oursky.authgear.getQueryList
import com.oursky.authgear.net.toQueryParameter

data class App2AppAuthenticateRequest(
    var authorizationEndpoint: String,
    var redirectUri: String,
    var clientID: String,
    var codeChallenge: String
) {
    companion object {
        fun parse(uri: Uri): App2AppAuthenticateRequest? {
            val queryList = uri.getQueryList()
            val queryMap = hashMapOf<String, String>()
            for (q in queryList) {
                queryMap[q.first] = q.second
            }
            val authorizationEndpoint: String = uri
                .buildUpon()
                .clearQuery()
                .build()
                .toString()
            val redirectUri: String = queryMap["redirect_uri"] ?: return null
            val clientID: String = queryMap["client_id"] ?: return null
            val codeChallenge: String = queryMap["code_challenge"] ?: return null
            return App2AppAuthenticateRequest(
                authorizationEndpoint = authorizationEndpoint,
                redirectUri = redirectUri,
                clientID = clientID,
                codeChallenge = codeChallenge
            )
        }
    }
}

internal fun App2AppAuthenticateRequest.toUri(): Uri {
    val query: Map<String, String> = hashMapOf(
        "client_id" to clientID,
        "redirect_uri" to this.redirectUri,
        "code_challenge_method" to AuthgearCore.CODE_CHALLENGE_METHOD,
        "code_challenge" to codeChallenge
    )
    return Uri.parse(this.authorizationEndpoint).buildUpon()
        .encodedQuery(query.toQueryParameter())
        .build()
}