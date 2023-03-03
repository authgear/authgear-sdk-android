package com.oursky.authgear.latte.fragment

import android.net.Uri

internal class LatteWebViewFragment(id: String, url: Uri, redirectUri: String) :
    LatteFragment<Unit>(id, url, redirectUri) {

    override suspend fun onHandleFinishUri(finishUri: Uri) {
    }
}