package com.oursky.authgear

internal fun formatOAuthExceptionMessage(error: String, errorDescription: String?): String {
    var message = error
    if (errorDescription != null) {
        message += ": " + errorDescription
    }
    return message
}

class OAuthException : AuthgearException {
    val error: String
    val errorDescription: String?
    var state: String?
    var errorURI: String?

    constructor(error: String, errorDescription: String?, state: String?, errorURI: String?) : super(formatOAuthExceptionMessage(error, errorDescription)) {
        this.error = error
        this.errorDescription = errorDescription
        this.state = state
        this.errorURI = errorURI
    }
}
