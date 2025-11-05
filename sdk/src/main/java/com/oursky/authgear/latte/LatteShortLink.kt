package com.oursky.authgear.latte

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.net.DefaultHTTPClient
import com.oursky.authgear.net.HTTPClientHelper
import com.oursky.authgear.net.HTTPRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.KITKAT)
class LatteShortLink(val uri: Uri, val appLinkOrigin: Uri, val rewriteAppLinkOrigin: Uri?) {
    suspend fun expand(): LatteAppLink? {
        return withContext(Dispatchers.IO) {
            val url = URL(uri.toString())
            val req = HTTPRequest(
                method = "GET",
                headers = hashMapOf(),
                uri = URI(url.toString()),
                followRedirect = false
            )
            val httpClient = DefaultHTTPClient()
            val resp = httpClient.send(req)
            val responseString = resp.body.use {
                String(it.readBytes(), StandardCharsets.UTF_8)
            }
            HTTPClientHelper.throwErrorIfNeeded(resp.statusCode, responseString)
            val locationList = resp.headers["Location"] ?: throw LatteException.InvalidShortLink
            if (locationList.isEmpty()) {
                throw LatteException.InvalidShortLink
            }
            val location = locationList[0]
            val uri = Uri.parse(location)
            return@withContext LatteAppLink.create(uri, appLinkOrigin, rewriteAppLinkOrigin)

        }
    }
}
