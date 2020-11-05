package com.oursky.authgear

interface OnSessionStateChangedListener {
    fun onSessionStateChanged(container: Authgear, reason: SessionStateChangeReason)
}
