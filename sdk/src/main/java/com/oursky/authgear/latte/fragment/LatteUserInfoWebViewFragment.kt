package com.oursky.authgear.latte.fragment

import android.net.Uri
import com.oursky.authgear.*

internal class LatteUserInfoWebViewFragment(id: String, url: Uri, redirectUri: String, webContentsDebuggingEnabled: Boolean) :
    LatteFragment<UserInfo>(id, url, redirectUri, webContentsDebuggingEnabled) {

    override suspend fun onHandleFinishUri(finishUri: Uri): UserInfo {
        val latte = latte ?: throw CancelException()
        return latte.authgear.fetchUserInfo()
    }
}