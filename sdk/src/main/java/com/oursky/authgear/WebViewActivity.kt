package com.oursky.authgear

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

internal class WebViewActivity : AppCompatActivity() {
    companion object {
        fun createIntent(context: Context, uri: Uri): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = uri
            return intent
        }
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val deepLink = request?.url.toString()
                if (AuthgearCore.handleWeChatRedirectDeepLink(deepLink)) {
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        val url = intent.data.toString()
        webView.loadUrl(url)
        setContentView(webView)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
