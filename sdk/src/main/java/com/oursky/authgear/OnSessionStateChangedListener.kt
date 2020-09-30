package com.oursky.authgear

interface OnSessionStateChangedListener {
    fun onSessionStateChanged(state: SessionState, reason: SessionStateChangeReason)
}
