package com.oursky.authgear.latte.fragment

import android.net.Uri
import com.oursky.authgear.*

internal class LatteUserInfoWebViewFragment(url: Uri, redirectUri: String) :
    LatteFragment<UserInfo>(url, redirectUri) {

    override suspend fun onHandleFinishUri(finishUri: Uri): UserInfo {
        val latte = latte ?: throw CancelException()
        return latte.authgear.fetchUserInfo()
    }
}