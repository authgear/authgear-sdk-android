package com.oursky.authgear.latte

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.annotation.RequiresApi

internal class WebViewClient(
    private val webView: WebView
) : android.webkit.WebViewClient() {
    private fun removeQueryAndFragment(uri: Uri): Uri {
        return uri.buildUpon().query(null).fragment(null).build()
    }

    private fun checkRedirectURI(uri: Uri): Boolean {
        val redirectURI = this.webView.request.redirectUri
        val currentURI = removeQueryAndFragment(uri)
        if (redirectURI == currentURI.toString()) {
            webView.listener?.onComplete(webView, Result.success(WebViewResult(uri)))
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
        super.onPageStarted(view, url, favicon)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: android.webkit.WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        // For unknown reason, when we attempt to inspect the webview with chrome,
        // the webview will receive this error.
        // But this is not a critical error so we ignore it here.
        if (error != null && error.errorCode == ERROR_UNKNOWN && error.description == "net::ERR_CACHE_MISS") {
            return
        }
        webView.listener?.onComplete(webView, Result.failure(WebViewError(request!!, error!!)))
    }

    override fun onReceivedError(
        view: android.webkit.WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        // For unknown reason, when we attempt to inspect the webview with chrome,
        // the webview will receive this error.
        // But this is not a critical error so we ignore it here.
        if (errorCode == ERROR_UNKNOWN && description == "net::ERR_CACHE_MISS") {
            return
        }
        webView.listener?.onComplete(webView, Result.failure(WebViewError(errorCode, description!!, failingUrl!!)))
    }
}
