package com.oursky.authgear.data.assetlink

import android.net.Uri

internal interface AssetLinkRepo {
    fun getAssetLinks(domain: Uri): List<AssetLink>
}