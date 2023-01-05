package com.oursky.authgear

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.atomic.AtomicInteger

class OAuthWebViewActivity : AppCompatActivity() {

    companion object {
        private const val KEY_REDIRECT_URI = "redirect_uri"
        private const val MENU_ID_CANCEL = 1
        fun createIntent(ctx: Context?, uri: Uri?, redirectUri: Uri?): Intent {
            val intent = Intent(ctx, OAuthWebViewActivity::class.java)
            // It is important to keep activity launch mode as standard.
            // Other launch modes such as singleTask will make onActivityResult called immediately
            // with RESULT_CANCELED.
            intent.data = uri
            intent.putExtra(KEY_REDIRECT_URI, redirectUri)
            return intent
        }
    }

    private var mWebView: WebView? = null
    private val mFileChooserCallback = SparseArray<ValueCallback<Array<Uri>>>()
    private val mRequestCode = AtomicInteger()
    private var mResult: Intent? = null

    override fun finish() {
        // finish() is overridden to ALWAYS set activity result.
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
        mWebView = WebView(this)
        mWebView!!.settings.javaScriptEnabled = true

        // Override URL navigation
        mWebView!!.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (mWebView!!.canGoBack()) {
                    // Show back button in the toolbar.
                    this@OAuthWebViewActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                } else {
                    this@OAuthWebViewActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                }
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val redirectURI = this@OAuthWebViewActivity.intent.getParcelableExtra<Uri>(
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
                    this@OAuthWebViewActivity.startActivityForResult(intent, requestCode)
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
}
