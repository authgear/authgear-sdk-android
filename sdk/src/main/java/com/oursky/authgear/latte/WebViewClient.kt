package com.oursky.authgear.latte

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import androidx.annotation.RequiresApi

internal class WebViewClient(
    private val webView: WebView,
    private val inner: android.webkit.WebViewClient
) : android.webkit.WebViewClient() {
    private fun removeQueryAndFragment(uri: Uri): Uri {
        return uri.buildUpon().query(null).fragment(null).build()
    }

    private fun checkRedirectURI(uri: Uri): Boolean {
        val redirectURI = this.webView.request.redirectUri
        val currentURI = removeQueryAndFragment(uri)
        if (redirectURI == currentURI.toString()) {
            webView.completion?.invoke(webView, Result.success(WebViewResult(uri)))
            webView.completion = null
            return true
        }
        return false
    }

    override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean {
        return super.shouldOverrideUrlLoading(view, url) || this.checkRedirectURI(Uri.parse(url))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(
        view: android.webkit.WebView,
        request: WebResourceRequest
    ): Boolean {
        return super.shouldOverrideUrlLoading(view, request) || this.checkRedirectURI(request.url)
    }

    override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: Bitmap?) {
        view?.evaluateJavascript(WebViewJSInterface.initScript) {}
        view?.requestFocus()
        inner.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
        inner.onPageFinished(view, url)
    }

    // TODO: delegate all methods to inner?
}
