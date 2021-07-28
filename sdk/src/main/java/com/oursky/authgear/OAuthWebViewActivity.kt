package com.oursky.authgear

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

internal class OAuthWebViewActivity : AppCompatActivity() {
    companion object {
        private const val MENU_ID_CANCEL = 1
        private const val KEY_REDIRECT_URL = "redirectUrl"
        private const val KEY_AUTHORIZATION_URL = "authorizationUrl"

        fun createIntent(context: Context, redirectUrl: String, url: String): Intent {
            val intent = Intent(context, OAuthWebViewActivity::class.java)
            intent.putExtra(KEY_REDIRECT_URL, redirectUrl)
            intent.putExtra(KEY_AUTHORIZATION_URL, url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                if (AuthgearCore.handleWechatRedirectDeepLink(deepLink)) {
                    return true
                }
                intent.getStringExtra(KEY_REDIRECT_URL)?.let {
                    val deepLinkWithoutQuery = getURLWithoutQuery(deepLink)
                    if (deepLinkWithoutQuery == it) {
                        AuthgearCore.handleDeepLink(deepLink, true)
                        finish()
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        intent.getStringExtra(KEY_AUTHORIZATION_URL)?.let {
            webView.loadUrl(it)
        }
        setContentView(webView)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            intent.getStringExtra(KEY_REDIRECT_URL)?.let {
                // User cancelled
                AuthgearCore.handleDeepLink(it, false)
            }
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(Menu.NONE, MENU_ID_CANCEL, Menu.NONE, R.string.cancel)
            ?.setIcon(R.drawable.ic_menu_close_clear_cancel)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        MENU_ID_CANCEL -> {
            intent.getStringExtra(KEY_REDIRECT_URL)?.let {
                // User cancelled
                AuthgearCore.handleDeepLink(it, false)
            }
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun getURLWithoutQuery(input: String): String {
        val uri = Uri.parse(input)
        var builder = uri.buildUpon().clearQuery()
        builder = builder.fragment("")
        return builder.build().toString()
    }
}
