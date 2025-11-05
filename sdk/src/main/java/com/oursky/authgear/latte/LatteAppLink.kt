package com.oursky.authgear.latte

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.net.HTTPClientHelper
import com.oursky.authgear.getOrigin
import com.oursky.authgear.net.HTTPRequest
import com.oursky.authgear.rewriteOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.KITKAT)
sealed class LatteAppLink {

    class ResetLink(val uri: Uri) : LatteAppLink()

    class LoginLink(val uri: Uri) : LatteAppLink() {
        suspend fun handle(latte: Latte) {
            return withContext(Dispatchers.IO) {
                val url = URL(uri.toString())
                val req = HTTPRequest(
                    method = "POST",
                    headers = hashMapOf(),
                    uri = URI(url.toString()),
                )
                val resp = latte.httpClient.send(req)
                val responseString = resp.body.use {
                    String(it.readBytes(), StandardCharsets.UTF_8)
                }
                HTTPClientHelper.throwErrorIfNeeded(resp.statusCode, responseString)
            }
        }
    }

    companion object {
        fun create(uri: Uri, appLinkOrigin: Uri, rewriteAppLinkOrigin: Uri?): LatteAppLink? {
            var linkUri = uri
            val origin = uri.getOrigin() ?: return null
            val path = uri.path ?: return null
            if (origin != appLinkOrigin.getOrigin()) {
                return null
            }
            if (rewriteAppLinkOrigin != null) {
                linkUri = uri.rewriteOrigin(rewriteAppLinkOrigin)
            }

            val link = when {
                path.endsWith("/reset_link") -> ResetLink(linkUri)
                path.endsWith("/login_link") -> LoginLink(linkUri)
                else -> null
            }

            return link
        }
    }
}
