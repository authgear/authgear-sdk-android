package com.oursky.authgear

data class ConfigureOptions(
    /**
     * The OAuth client ID.
     */
    val clientId: String,

    /**
     * The endpoint of authgear server.
     */
    val endpoint: String
)