package com.oursky.authgear

/**
 * The session state.
 *
 * An freshly constructed instance has the session state [SessionState.UNKNOWN].
 *
 * After a call to configure, the session state would become [SessionState.AUTHENTICATED] if a previous session was found,
 * or [SessionState.NO_SESSION] if such session was not found.

 */
enum class SessionState {
    UNKNOWN,
    NO_SESSION,
    AUTHENTICATED
}
