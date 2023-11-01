package com.oursky.authgear.latte

import com.oursky.authgear.AuthgearException

sealed class LatteException(message: String) : AuthgearException(message = message) {
    object InvalidShortLink : LatteException("invalid short link")
    object Timeout : LatteException("timeout")
}
