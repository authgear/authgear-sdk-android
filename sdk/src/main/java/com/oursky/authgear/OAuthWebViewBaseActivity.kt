package com.oursky.authgear

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.atomic.AtomicInteger

internal open class OAuthWebViewBaseActivity : AppCompatActivity() {

    companion object {
        internal const val KEY_REDIRECT_URI = "redirectUri"
        internal const val KEY_BROADCAST_ACTION = "broadcastAction"
        private const val MENU_ID_CANCEL = 1
    }

    private var mWebView: AuthgearWebView? = null
    private val mFileChooserCallback = SparseArray<ValueCallback<Array<Uri>>>()
    private val mRequestCode = AtomicInteger()
    private var mResult: Intent? = null

    override fun finish() {
        // finish() is overridden to ALWAYS set activity result.
        this.sendRedirectURLBroadcast()
        if (mResult == null) {
            this.setResult(RESULT_CANCELED)
        } else {
            this.setResult(RESULT_OK, mResult)
        }
        super.finish()
    }

    override fun onBackPressed() {
        // onBackPressed to overridden to handle hardware back button.
        if (mWebView!!.canGoBack()) {
            mWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // onSupportNavigateUp is override to handle the back button in the toolbar.
        return if (mWebView!!.canGoBack()) {
            mWebView!!.goBack()
            false
        } else {
            finish()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // onCreateOptionsMenu is overridden to add a cancel button in the toolbar.
        val item = menu.add(Menu.NONE, MENU_ID_CANCEL, Menu.NONE, android.R.string.cancel)
        item.setIcon(R.drawable.baseline_close_36)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // onOptionsItemSelected is overridden to handle the cancel button in the toolbar.
        return if (item.itemId == MENU_ID_CANCEL) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mWebView = AuthgearWebView(this)

        // Override URL navigation
        val webViewClient = object : AuthgearWebView.Client() {
            override fun onPageFinished(view: WebView, url: String) {
                if (mWebView!!.canGoBack()) {
                    // Show back button in the toolbar.
                    this@OAuthWebViewBaseActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                } else {
                    this@OAuthWebViewBaseActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val redirectURI = this@OAuthWebViewBaseActivity.intent.getParcelableExtra<Uri>(
                    KEY_REDIRECT_URI
                )
                val currentURI = removeQueryAndFragment(request.url)
                if (redirectURI == currentURI) {
                    val intent = Intent()
                    intent.data = request.url
                    mResult = intent
                    finish()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        mWebView!!.setAuthgearWebViewClient(webViewClient)
        mWebView!!.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (filePathCallback != null && fileChooserParams != null) {
                    val intent = fileChooserParams.createIntent()
                    val requestCode = mRequestCode.incrementAndGet()
                    mFileChooserCallback.setValueAt(requestCode, filePathCallback)
                    this@OAuthWebViewBaseActivity.startActivityForResult(intent, requestCode)
                    // When this method returns true,
                    // filePathCallback must be called.
                    // https://developer.android.com/reference/android/webkit/WebChromeClient#onShowFileChooser(android.webkit.WebView,%20android.webkit.ValueCallback%3Candroid.net.Uri[]%3E,%20android.webkit.WebChromeClient.FileChooserParams)
                    return true
                }
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }
        }
        val data = this.intent.data
        mWebView!!.loadUrl(data.toString())
        this.setContentView(mWebView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val callback = mFileChooserCallback[requestCode] ?: return
        mFileChooserCallback.remove(requestCode)

        // When callback is present, it must be called.
        if (resultCode == RESULT_OK && data != null && data.data != null) {
            callback.onReceiveValue(arrayOf(data.data!!))
        } else {
            callback.onReceiveValue(null)
        }
    }

    private fun removeQueryAndFragment(uri: Uri): Uri {
        return uri.buildUpon().query(null).fragment(null).build()
    }

    private fun sendRedirectURLBroadcast() {
        intent.getStringExtra(KEY_BROADCAST_ACTION)?.let { broadcastAction ->
            val broadcastIntent = Intent(broadcastAction)
            broadcastIntent.setPackage(this.applicationContext.packageName)
            broadcastIntent.putExtra(AuthgearCore.KEY_OAUTH_BOARDCAST_TYPE, OAuthBroadcastType.REDIRECT_URL.name)
            mResult?.data?.toString()?.let {
                broadcastIntent.putExtra(AuthgearCore.KEY_REDIRECT_URL, it)
            }
            this.sendBroadcast(broadcastIntent)
        }
    }
}
