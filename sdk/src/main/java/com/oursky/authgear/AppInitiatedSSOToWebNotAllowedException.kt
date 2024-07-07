package com.oursky.authgear

sealed class AppInitiatedSSOToWebNotAllowedException : AuthgearException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    class InsufficientScopeException: AppInitiatedSSOToWebNotAllowedException()
    class IDTokenNotFoundException: AppInitiatedSSOToWebNotAllowedException()
    class DeviceSecretNotFoundException: AppInitiatedSSOToWebNotAllowedException()
}
