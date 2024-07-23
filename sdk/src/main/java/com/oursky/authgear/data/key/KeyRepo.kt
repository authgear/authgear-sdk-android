package com.oursky.authgear.data.key

import java.security.KeyPair

internal interface KeyRepo {
    fun generateAnonymousKey(kid: String): KeyPair
    fun getAnonymousKey(kid: String): KeyPair?

    fun generateApp2AppDeviceKey(kid: String): KeyPair
    fun getApp2AppDeviceKey(kid: String): KeyPair?

    fun generateDPoPKey(kid: String): KeyPair
    fun getDPoPKey(kid: String): KeyPair?
}
