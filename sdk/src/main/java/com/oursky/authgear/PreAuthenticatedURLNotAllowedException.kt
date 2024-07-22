package com.oursky.authgear

open class PreAuthenticatedURLNotAllowedException : AuthgearException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class PreAuthenticatedURLNotAllowedInsufficientScopeException: PreAuthenticatedURLNotAllowedException()
class PreAuthenticatedURLNotAllowedIDTokenNotFoundException: PreAuthenticatedURLNotAllowedException()
class PreAuthenticatedURLNotAllowedDeviceSecretNotFoundException: PreAuthenticatedURLNotAllowedException()
