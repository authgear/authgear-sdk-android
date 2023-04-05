package com.oursky.authgear.latte.fragment

import android.net.Uri
import android.os.Bundle
import com.oursky.authgear.*

internal class LatteUserInfoWebViewFragment() : LatteFragment<UserInfo>() {

    internal constructor(url: Uri, redirectUri: String) : this() {
        arguments = arguments ?: Bundle()
        requireArguments().apply {
            putString(KEY_URL, url.toString())
            putString(KEY_REDIRECT_URI, redirectUri)
        }
    }
    override suspend fun onHandleFinishUri(finishUri: Uri): UserInfo {
        val latte = latte ?: throw CancelException()
        return latte.authgear.fetchUserInfo()
    }
}