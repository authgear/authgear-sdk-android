package com.oursky.authgear.latte.fragment

import android.net.Uri

internal class LatteWebViewFragment(id: String, url: Uri, redirectUri: String, webContentsDebuggingEnabled: Boolean) :
    LatteFragment<Unit>(id, url, redirectUri, webContentsDebuggingEnabled) {

    override suspend fun onHandleFinishUri(finishUri: Uri) {
    }
}