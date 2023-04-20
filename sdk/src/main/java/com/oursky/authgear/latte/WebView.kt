package com.oursky.authgear.latte

import android.annotation.SuppressLint
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.oursky.authgear.AuthgearException

@SuppressLint("SetJavaScriptEnabled")
internal class WebView(context: Context, webContentsDebuggingEnabled: Boolean) : android.webkit.WebView(context) {
    var request: WebViewRequest? = null
    var listener: WebViewListener? = null

    init {
        addJavascriptInterface(WebViewJSInterface(this), WebViewJSInterface.jsBridgeName)
        settings.javaScriptEnabled = true

        if (webContentsDebuggingEnabled) {
            setWebContentsDebuggingEnabled(true)
        }

        webViewClient = android.webkit.WebViewClient()
        // TODO: copy WebChromeClient from OAuthWebViewBaseActivity?
    }

    fun load() {
        val url = this.request?.url ?: throw AuthgearException("request not provided")
        this.loadUrl(url.toString())
        this.requestFocus()
    }

    override fun setWebViewClient(client: android.webkit.WebViewClient) {
        super.setWebViewClient(WebViewClient(this, client))
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        val conn = super.onCreateInputConnection(outAttrs)
        post { showSoftInputIfNeeded() }
        return conn
    }

    private fun showSoftInputIfNeeded() {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (manager.isAcceptingText) {
            manager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}