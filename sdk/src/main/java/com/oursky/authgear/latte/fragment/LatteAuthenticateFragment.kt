package com.oursky.authgear.latte.fragment

import android.net.Uri
import android.os.Bundle
import com.oursky.authgear.*

@OptIn(ExperimentalAuthgearApi::class)
internal class LatteAuthenticateFragment() : LatteFragment<UserInfo>() {

    companion object {
        const val KEY_AUTH_REQUEST = "auth_request"
    }

    internal constructor(request: AuthenticationRequest) : this() {
        arguments = arguments ?: Bundle()
        requireArguments().apply {
            putBundle(KEY_AUTH_REQUEST, request.toBundle())
            putString(KEY_URL, request.url.toString())
            putString(KEY_REDIRECT_URI, request.redirectUri)
        }
    }

    override suspend fun onHandleFinishUri(finishUri: Uri): UserInfo {
        val latte = latte ?: throw CancelException()
        val request = AuthenticationRequest(requireArguments().getBundle(KEY_AUTH_REQUEST)!!)
        return latte.authgear.finishAuthentication(finishUri.toString(), request)
    }
}