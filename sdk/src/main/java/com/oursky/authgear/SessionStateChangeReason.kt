package com.oursky.authgear

/**
 * The reason that [SessionState] is changed in a user session represented by an authgear instance.
 *
 * These reasons can be thought of as the transition of a [SessionState], which is described as
 * follows:
 * ```
 *                                                              Logout/Expiry
 *                                                  +-----------------------------------------+
 *                                                  v                                         |
 *  SessionState.Unknown --- NoToken ----> SessionState.NoSession ---- Authorized -----> SessionState.LoggedIn
 *      |                                                                                    ^
 *      +------------------------------------------------------------------------------------+
 *                                              FoundToken
 * ```
 *
 * These transitions and states can be used in [OnSessionStateChangedListener] to examine the
 * current state of authgear.
 *
 * For example, to check if a user is logged out, check if the new state is [SessionState.NoSession]
 * and reason [SessionStateChangeReason.Logout].
 *
 * To check if the session is expired, check if the new state is [SessionState.NoSession] and
 * reason [SessionStateChangeReason.Expired].
 *
 * The same can be done for login. A [SessionState.LoggedIn] with
 * [SessionStateChangeReason.Authorized] means the user had just logged in,
 * or if the reason is [SessionStateChangeReason.FoundToken] instead, a previous session of the user
 * is found.
 */
enum class SessionStateChangeReason {
    NoToken,
    FoundToken,
    Authorized,
    Logout,
    Expired
}
