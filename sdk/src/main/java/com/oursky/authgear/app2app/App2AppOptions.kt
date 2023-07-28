package com.oursky.authgear.app2app

// These options defines how the sdk handles incoming app2app requests
data class App2AppOptions constructor(
    /**
     * If true, new sessions will be prepared for participating in app2app authentication
    */
    var isEnabled: Boolean,
    /**
     * If true, the sdk will try to create and bind device key during refresh tokens to "migrate" to app2app sessions
    */
    var isInsecureDeviceKeyBindingEnabled: Boolean = false
)
