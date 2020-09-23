package com.oursky.authgear.data.key

interface KeyRepo {
    fun getAnonymousKey(kid: String?, isUrlSafe: Boolean = true): JwkResponse
    fun signAnonymousToken(kid: String, data: String, isUrlSafe: Boolean = true): String
}
