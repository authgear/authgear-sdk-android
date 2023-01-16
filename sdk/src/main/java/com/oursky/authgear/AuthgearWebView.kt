package com.oursky.authgear

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class AuthgearWebView(
    context: Context,
    private val listener: AuthgearWebViewListener
) : WebView(context) {
    companion object {
        private const val jsBridgeName = "authgear"
        private val script = """
            if (!window.onOpenEmailClientListenerAdded) {
                document.addEventListener('authgear:onOpenEmailClient',
                    function(){
                        window.$jsBridgeName.onOpenEmailClient();
                    }
                )
            }
            window.onOpenEmailClientListenerAdded = true
        """.trimIndent()
    }

    private val jSBridge = object {
        @JavascriptInterface
        fun onOpenEmailClient() {
            listener.onOpenEmailClient()
        }
    }

    init {
        setAuthgearWebViewClient(Client())
        addJavascriptInterface(jSBridge, jsBridgeName)
        settings.javaScriptEnabled = true
    }

    fun setAuthgearWebViewClient(client: Client) {
        super.setWebViewClient(client)
    }

    open class Client : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            view.evaluateJavascript(script) {}
        }
    }
}