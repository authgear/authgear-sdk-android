package com.oursky.authgear.data.assetlink

import android.net.Uri

internal interface AssetLinkRepo {
    fun getAssetLinks(origin: Uri): List<AssetLink>
}