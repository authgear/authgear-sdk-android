package com.oursky.authgear.latte.fragment

import android.net.Uri

internal class LatteWebViewFragment(url: Uri, redirectUri: String) :
    LatteFragment<Unit>(url, redirectUri) {

    override suspend fun onHandleFinishUri(finishUri: Uri) {
    }
}