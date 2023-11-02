package com.oursky.authgear

interface AuthgearDelegate {
    fun onSessionStateChanged(container: Authgear, reason: SessionStateChangeReason) {}

    fun sendWechatAuthRequest(state: String) {}
}
