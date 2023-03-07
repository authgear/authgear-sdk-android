package com.oursky.authgear.latte.fragment

import android.net.Uri
import com.oursky.authgear.*

@OptIn(ExperimentalAuthgearApi::class)
internal class LatteAuthenticateFragment(id: String, request: AuthenticationRequest) :
    LatteFragment<UserInfo>(id, request.url, request.redirectUri) {

    companion object {
        const val KEY_AUTH_REQUEST = "auth_request"
    }

    init {
        requireArguments().putBundle(KEY_AUTH_REQUEST, request.toBundle())
    }

    override suspend fun onHandleFinishUri(finishUri: Uri): UserInfo {
        val latte = latte ?: throw CancelException()
        val request = AuthenticationRequest(requireArguments().getBundle(KEY_AUTH_REQUEST)!!)
        return latte.authgear.finishAuthentication(finishUri.toString(), request)
    }
}