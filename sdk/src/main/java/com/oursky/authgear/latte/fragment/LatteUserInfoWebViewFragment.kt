package com.oursky.authgear.latte.fragment

import android.net.Uri
import com.oursky.authgear.*

internal class LatteUserInfoWebViewFragment(id: String, url: Uri, redirectUri: String, webviewIsInspectable: Boolean) :
    LatteFragment<UserInfo>(id, url, redirectUri, webviewIsInspectable) {

    override suspend fun onHandleFinishUri(finishUri: Uri): UserInfo {
        val latte = latte ?: throw CancelException()
        return latte.authgear.fetchUserInfo()
    }
}