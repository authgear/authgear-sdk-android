package com.oursky.authgear

interface AuthgearDelegate {
    @JvmDefault
    fun onSessionStateChanged(container: Authgear, reason: SessionStateChangeReason) {}

    @JvmDefault
    fun sendWechatAuthRequest(state: String) {}
}
