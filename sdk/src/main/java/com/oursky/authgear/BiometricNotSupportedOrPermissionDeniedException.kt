package com.oursky.authgear

class BiometricNotSupportedOrPermissionDeniedException : AuthgearException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}