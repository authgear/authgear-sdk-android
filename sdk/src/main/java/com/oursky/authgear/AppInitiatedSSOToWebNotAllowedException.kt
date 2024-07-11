package com.oursky.authgear

sealed class PreAuthenticatedURLNotAllowedException : AuthgearException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    class InsufficientScopeException: PreAuthenticatedURLNotAllowedException()
    class IDTokenNotFoundException: PreAuthenticatedURLNotAllowedException()
    class DeviceSecretNotFoundException: PreAuthenticatedURLNotAllowedException()
}
