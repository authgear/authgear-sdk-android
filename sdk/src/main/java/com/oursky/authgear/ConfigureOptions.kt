package com.oursky.authgear

data class ConfigureOptions @JvmOverloads constructor(
    /**
     * When it is not needed to always have the most up-to-date access token during application
     * setup, set [skipRefreshAccessToken] to `true` to bypass refreshing access token. You can
     * later use [refreshAccessTokenIfNeeded] or the synchronous variant
     * [refreshAccessTokenIfNeededSync] to refresh when necessary.
     *
     *  It is recommended that the application setup a proper http request abstraction that calls
     * [refreshAccessTokenIfNeededSync] (or its async variant) so that the validity
     * of access token is handled automatically.
     */
    var skipRefreshAccessToken: Boolean = false,

    /**
     * Indicate if the session is transient. If session is transient, refresh token will be stored
     * in memory, so it will be discard when the app is killed.
     */
    var transientSession: Boolean = false
)
