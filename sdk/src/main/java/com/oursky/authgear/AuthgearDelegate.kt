package com.oursky.authgear

interface AuthgearDelegate {
    // error is non-null when reason is INVALID, i.e. the session was cleared
    // because a request failed with an error such as invalid_grant or
    // invalid_dpop_proof. It is null for all other reasons.
    fun onSessionStateChanged(container: Authgear, reason: SessionStateChangeReason, error: Throwable?) {}

    fun sendWechatAuthRequest(state: String) {}
}
