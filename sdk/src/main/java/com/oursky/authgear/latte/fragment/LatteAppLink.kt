package com.oursky.authgear.latte.fragment

import android.net.Uri
import com.oursky.authgear.data.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.StandardCharsets

sealed class LatteAppLink {
    abstract suspend fun handle(presenter: LattePresenter)

    internal class ResetLink(val query: List<Pair<String, String>>) : LatteAppLink() {
        override suspend fun handle(presenter: LattePresenter) {
            val delegate = presenter.delegate ?: return
            val handle = presenter.resetPassword(query)
            delegate.showLatteFragment(handle.id, handle.fragment)
            try {
                handle.await()
            } finally {
                delegate.hideLatteFragment(handle.id)
            }
        }
    }

    internal class LoginLink(val uri: Uri) : LatteAppLink() {
        override suspend fun handle(presenter: LattePresenter) {
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