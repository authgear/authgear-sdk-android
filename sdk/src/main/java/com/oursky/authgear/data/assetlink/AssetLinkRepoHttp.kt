package com.oursky.authgear.data.assetlink

import android.net.Uri
import com.oursky.authgear.net.HTTPClientHelper
import com.oursky.authgear.net.HTTPClient
import com.oursky.authgear.net.HTTPRequest
import java.net.URI
import java.nio.charset.StandardCharsets

internal class AssetLinkRepoHttp(private val httpClient: HTTPClient) : AssetLinkRepo {
    companion object {
        @Suppress("unused")
        private val TAG = AssetLinkRepoHttp::class.java.simpleName
    }

    override fun getAssetLinks(origin: Uri): List<AssetLink> {
        val assetLinkUri = origin.buildUpon()
            .path("/.well-known/assetlinks.json")
            .clearQuery()
            .build()

        val request = HTTPRequest(
            method = "GET",
            headers = hashMapOf(),
            uri = URI(assetLinkUri.toString()),
        )

        val response = this.httpClient.send(request)
        val responseString = response.body.use {
            String(it.readBytes(), StandardCharsets.UTF_8)
        }
        HTTPClientHelper.throwErrorIfNeeded(response.statusCode, responseString)
        val result: List<AssetLink> = HTTPClientHelper.json.decodeFromString(responseString)

        return result
    }
}
