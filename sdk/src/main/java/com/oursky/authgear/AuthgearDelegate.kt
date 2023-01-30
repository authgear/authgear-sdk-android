package com.oursky.authgear

import android.content.Context

interface AuthgearDelegate {
    @JvmDefault
    fun onSessionStateChanged(container: Authgear, reason: SessionStateChangeReason) {}

    @JvmDefault
    fun sendWechatAuthRequest(state: String) {}

    @JvmDefault
    fun onOpenEmailClient(context: Context) {}
}
