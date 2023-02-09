package com.oursky.authgear

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

internal class WebViewActivity : AppCompatActivity() {
    companion object {
        private const val MENU_ID_CANCEL = 1
        private const val KEY_BROADCAST_ACTION = "broadcastAction"
        private const val REQUEST_CODE_CHOOSE_FILE = 1
        const val KEY_BROADCAST_TYPE = "broadcastType"

        fun createIntent(context: Context, broadcastAction: String, uri: Uri): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = uri
            intent.putExtra(KEY_BROADCAST_ACTION, broadcastAction)
            return intent
        }
    }

    private lateinit var webView: AuthgearWebView
    private var cachedFilePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = AuthgearWebView(this, object : AuthgearWebViewListener {
            override fun onOpenEmailClient() {
                sendOpenEmailClientBroadcast()
            }
        })
        webView.setAuthgearWebViewClient(object : AuthgearWebView.Client() {
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
        })
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback != null && fileChooserParams != null) {
                    cachedFilePathCallback = filePathCallback
                    val requestCode = REQUEST_CODE_CHOOSE_FILE
                    val intent = fileChooserParams.createIntent()
                    this@WebViewActivity.startActivityForResult(intent, requestCode)
                    return true
                }

                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
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
            this.sendEndBroadcast()
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
            this.sendEndBroadcast()
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE_FILE) {
            cachedFilePathCallback?.onReceiveValue(when (resultCode) {
                RESULT_OK -> {
                    if (data?.data != null) arrayOf(data.data!!) else null
                }
                else -> null
            })
        }
    }

    private fun sendEndBroadcast() {
        this.intent.getStringExtra(KEY_BROADCAST_ACTION)?.let { broadcastAction ->
            val broadcastIntent = Intent(broadcastAction)
            broadcastIntent.putExtra(KEY_BROADCAST_TYPE, BroadcastType.END.name)
            this.sendBroadcast(broadcastIntent)
        }
    }

    private fun sendOpenEmailClientBroadcast() {
        this.intent.getStringExtra(KEY_BROADCAST_ACTION)?.let { broadcastAction ->
            val broadcastIntent = Intent(broadcastAction)
            broadcastIntent.putExtra(KEY_BROADCAST_TYPE, BroadcastType.OPEN_EMAIL_CLIENT.name)
            this.sendBroadcast(broadcastIntent)
        }
    }

    enum class BroadcastType {
        END,
        OPEN_EMAIL_CLIENT
    }
}
