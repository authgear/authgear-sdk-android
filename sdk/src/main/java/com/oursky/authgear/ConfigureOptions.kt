package com.oursky.authgear

data class ConfigureOptions @JvmOverloads constructor(
    /**
     * Indicate if the session is transient. If session is transient, refresh token will be stored
     * in memory, so it will be discard when the app is killed.
     */
    var transientSession: Boolean = false
)
