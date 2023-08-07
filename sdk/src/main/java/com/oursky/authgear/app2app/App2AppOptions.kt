package com.oursky.authgear.app2app

// These options defines how the sdk handles incoming app2app requests
data class App2AppOptions constructor(
    /**
     * If true, new sessions will be prepared for participating in app2app authentication
    */
    var isEnabled: Boolean
)
