package com.oursky.authgear.data.key

import java.security.KeyPair

internal interface KeyRepo {
    fun generateAnonymousKey(kid: String): KeyPair
    fun getAnonymousKey(kid: String): KeyPair?
}
