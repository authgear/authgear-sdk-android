package com.oursky.authgear

enum class SessionStateChangeReason {
    NoToken,
    FoundToken,
    Authorized,
    Logout,
    Expired
}
