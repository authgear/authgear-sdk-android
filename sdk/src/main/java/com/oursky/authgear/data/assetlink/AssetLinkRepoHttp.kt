package com.oursky.authgear.data.assetlink

import android.net.Uri
import com.oursky.authgear.data.HttpClient
import kotlinx.serialization.decodeFromString
import java.net.URL
import java.nio.charset.StandardCharsets

internal class AssetLinkRepoHttp : AssetLinkRepo {
    companion object {
        @Suppress("unused")
        private val TAG = AssetLinkRepoHttp::class.java.simpleName
    }

    override fun getAssetLinks(origin: Uri): List<AssetLink> {
        val assetLinkUri = origin.buildUpon()
            .path("/.well-known/assetlinks.json")
            .clearQuery()
            .build()
        val result: List<AssetLink> = HttpClient.fetch(
            url = URL(assetLinkUri.toString()),
            method = "GET",
            headers = hashMapOf()
        ) { conn ->
            conn.errorStream?.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
            }
            conn.inputStream.use {
                val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                HttpClient.throwErrorIfNeeded(conn, responseString)
                HttpClient.json.decodeFromString(responseString)
            }
        }
        return result
    }
}