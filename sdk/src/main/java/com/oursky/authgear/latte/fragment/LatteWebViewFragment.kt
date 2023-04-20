package com.oursky.authgear.latte.fragment

import android.net.Uri

internal class LatteWebViewFragment(id: String, url: Uri, redirectUri: String, webviewIsInspectable: Boolean) :
    LatteFragment<Unit>(id, url, redirectUri, webviewIsInspectable) {

    override suspend fun onHandleFinishUri(finishUri: Uri) {
    }
}