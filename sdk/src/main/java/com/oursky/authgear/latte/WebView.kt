package com.oursky.authgear.latte

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

@SuppressLint("SetJavaScriptEnabled")
internal class WebView(context: Context, val request: WebViewRequest, webContentsDebuggingEnabled: Boolean) : android.webkit.WebView(context) {
    var listener: WebViewListener? = null

    private var mIgnoreNextShowSoftInputIfNeeded: Boolean = false

    init {
        addJavascriptInterface(WebViewJSInterface(this), WebViewJSInterface.jsBridgeName)
        settings.javaScriptEnabled = true

        if (webContentsDebuggingEnabled) {
            setWebContentsDebuggingEnabled(true)
        }

        webViewClient = WebViewClient(this)
        // TODO: copy WebChromeClient from OAuthWebViewBaseActivity?
    }

    fun load() {
        val url = this.request.url
        this.loadUrl(url.toString())
    }

    fun dispatchSignal(signal: WebViewSignal) {
        evaluateJavascript("""
            document.dispatchEvent(
              new CustomEvent("latte:signal", {
                detail: { type: "${signal.value}" },
              })
            );
        """, null)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        val conn = super.onCreateInputConnection(outAttrs)
        post { showSoftInputIfNeeded() }
        return conn
    }

    private fun showSoftInputIfNeeded() {
        // On Huawei Nona 11 on EMUI 13.0.0, calling showSoftInput() will cause onCreateInputConnection to be called again, causing an infinite recursion.
        //
        // The observable consequence of this infinite recursion is that
        // the soft keyboard may sometimes disappear. After idling for a few minutes, the soft keyboard will crash, affecting the whole device.
        //
        // To work around this problem, we try to break the recursion with a boolean flag.
        // This workaround is based on an assumption that if the soft keyboard was just shown, or was shown already,
        // we can ignore an immediate request to show it again.
        if (this.mIgnoreNextShowSoftInputIfNeeded) {
            this.mIgnoreNextShowSoftInputIfNeeded = false
        } else {
            val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (manager.isAcceptingText) {
                manager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT,
                    object : ResultReceiver(Handler(Looper.myLooper()!!)) {
                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            if (resultCode == InputMethodManager.RESULT_SHOWN || resultCode == InputMethodManager.RESULT_UNCHANGED_SHOWN) {
                                this@WebView.mIgnoreNextShowSoftInputIfNeeded = true
                            }
                            super.onReceiveResult(resultCode, resultData)
                        }
                    })
            }
        }
    }
}
