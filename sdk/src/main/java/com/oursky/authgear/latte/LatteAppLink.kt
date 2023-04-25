package com.oursky.authgear.latte

import android.net.Uri
import com.oursky.authgear.data.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.StandardCharsets

sealed class LatteAppLink {

    class ResetLink(val uri: Uri) : LatteAppLink()

    class LoginLink(val uri: Uri) : LatteAppLink() {
        suspend fun handle(latte: Latte) {
            return withContext(Dispatchers.IO) {
                val url = URL(uri.toString())
                HttpClient.fetch(url, "POST", emptyMap()) { conn ->
                    conn.errorStream?.use {
                        val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                        HttpClient.throwErrorIfNeeded(conn, responseString)
                    }
                    conn.inputStream.use {
                        val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                        HttpClient.throwErrorIfNeeded(conn, responseString)
                    }
                }
            }
        }
    }
}