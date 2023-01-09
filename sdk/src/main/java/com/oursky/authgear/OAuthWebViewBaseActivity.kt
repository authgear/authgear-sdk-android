package com.oursky.authgear

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

internal open class OAuthWebViewBaseActivity : AppCompatActivity() {

    companion object {
        internal const val KEY_REDIRECT_URI = "redirectUri"
        internal const val KEY_BROADCAST_ACTION = "broadcastAction"
        private const val MENU_ID_CANCEL = 1
    }

    private var mWebView: WebView? = null
    private val mFileChooserCallback = SparseArray<ValueCallback<Array<Uri>>>()
    private val mRequestCode = AtomicInteger()
    private var mResult: Intent? = null
    private var safeAreaInsets: Insets = Insets.NONE
    private var isPageVisible = false

    override fun finish() {
        // finish() is overridden to ALWAYS set activity result.
        this.sendBroadcast()
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
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                this@OAuthWebViewBaseActivity.isPageVisible = true
                updateSafeArea()
                super.onPageCommitVisible(view, url)
            }

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

        ViewCompat.setOnApplyWindowInsetsListener(
            mWebView!!
        ) { view: View?, windowInsetsCompat: WindowInsetsCompat ->
            safeAreaInsets = windowInsetsCompat.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime()
            )
            updateSafeArea()
            windowInsetsCompat
        }
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

    private fun sendBroadcast() {
        intent.getStringExtra(KEY_BROADCAST_ACTION)?.let { broadcastAction ->
            val broadcastIntent = Intent(broadcastAction)
            mResult?.data?.toString()?.let {
                broadcastIntent.putExtra(AuthgearCore.KEY_REDIRECT_URL, it)
            }
            this.sendBroadcast(broadcastIntent)
        }
    }

    private fun updateSafeArea() {
        if (!this.isPageVisible) {
            return
        }

        // https://bugs.chromium.org/p/chromium/issues/detail?id=1094366
        val pixelRatio = resources.displayMetrics.density
        val setStyle = "document.documentElement.style.setProperty('%s', '%fpx')"
        val js = java.lang.String.join(
            ";",
            java.lang.String.format(
                Locale.ROOT,
                setStyle,
                "--safe-area-inset-top",
                safeAreaInsets.top / pixelRatio
            ),
            java.lang.String.format(
                Locale.ROOT,
                setStyle,
                "--safe-area-inset-left",
                safeAreaInsets.left / pixelRatio
            ),
            java.lang.String.format(
                Locale.ROOT,
                setStyle,
                "--safe-area-inset-right",
                safeAreaInsets.right / pixelRatio
            ),
            java.lang.String.format(
                Locale.ROOT,
                setStyle,
                "--safe-area-inset-bottom",
                safeAreaInsets.bottom / pixelRatio
            )
        )
        mWebView!!.evaluateJavascript(js, null)
    }
}
