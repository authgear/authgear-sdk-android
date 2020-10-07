package com.oursky.authgear

/**
 * The state of the user session in authgear. An authgear instance is in one and only one specific
 * state at any given point in time.
 *
 * For example, the session state of an authgear instance that is just constructed always has
 * [SessionState.Unknown]. After a call to [Authgear.configure], the session state would be
 * [SessionState.LoggedIn] if a previous session is found, or [SessionState.NoSession] if there is
 * no such sessions.
 *
 * [SessionStateChangeReason] are the transitions of this state machine and contains a more complete
 * state diagram. [OnSessionStateChangedListener] can be used to listen to state changes and inspect
 * the reason of the state change.
 */
enum class SessionState {
    LoggedIn,
    NoSession,
    Unknown
}
