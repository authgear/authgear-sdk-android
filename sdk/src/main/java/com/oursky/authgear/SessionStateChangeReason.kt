package com.oursky.authgear

/**
 * The reason why [SessionState] is changed.
 *
 * These reasons can be thought of as the transition of a [SessionState], which is described as follows:
 * ```
 *                                                              LOGOUT / INVALID
 *                                                  +-----------------------------------------+
 *                                                  v                                         |
 *  SessionState.UNKNOWN --- NO_TOKEN ----> SessionState.NO_SESSION ---- AUTHENTICATED -----> SessionState.AUTHENTICATED
 *      |                                                                                    ^
 *      +------------------------------------------------------------------------------------+
 *                                              FOUND_TOKEN
 * ```
 *
 */
enum class SessionStateChangeReason {
    NO_TOKEN,
    FOUND_TOKEN,
    AUTHENTICATED,
    LOGOUT,
    INVALID,
    CLEAR,
}
