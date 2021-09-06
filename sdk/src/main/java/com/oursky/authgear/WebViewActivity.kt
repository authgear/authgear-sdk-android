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

internal class WebViewActivity : AppCompatActivity() {
    companion object {
        private const val MENU_ID_CANCEL = 1
        private const val KEY_BROADCAST_ACTION = "broadcastAction"

        fun createIntent(context: Context, broadcastAction: String, uri: Uri): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = uri
            intent.putExtra(KEY_BROADCAST_ACTION, broadcastAction)
            return intent
        }
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
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
            this.sendBroadcast()
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
            this.sendBroadcast()
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun sendBroadcast() {
        this.intent.getStringExtra(KEY_BROADCAST_ACTION)?.let { broadcastAction ->
            val broadcastIntent = Intent(broadcastAction)
            this.sendBroadcast(broadcastIntent)
        }
    }
}
