package com.oursky.authgear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

internal class OauthActivity : AppCompatActivity() {
    companion object {
        @Suppress("unused")
        private val TAG = OauthActivity::class.java.simpleName
        private const val KEY_REDIRECT_URL = "redirectUrl"
        private const val KEY_AUTHORIZATION_URL = "authorizationUrl"
        /**
         * Create an intent to open a browser to perform login.
         */
        fun createAuthorizationIntent(context: Context, redirectUrl: String, url: String): Intent {
            val intent = Intent(context, OauthActivity::class.java)
            intent.putExtra(KEY_REDIRECT_URL, redirectUrl)
            intent.putExtra(KEY_AUTHORIZATION_URL, url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }
        /**
         * Create an intent to handle a deep link launched from browser.
         */
        fun createHandleDeepLinkIntent(context: Context, uri: Uri?): Intent {
            val intent = Intent(context, OauthActivity::class.java)
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return intent
        }
    }
    private var mIsBrowserOpened = false
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    override fun onResume() {
        super.onResume()
        // Either open the browser or finish with or without deep link.
        if (tryOpenBrowser()) return
        val deepLink = intent.data?.toString()
        if (deepLink != null) {
            AuthgearCore.handleDeepLink(deepLink, true)
        } else {
            intent.getStringExtra(KEY_REDIRECT_URL)?.let {
                AuthgearCore.handleDeepLink(it, false)
            }
        }
        finish()
    }
    /**
     * @return `true` if a browser is opened due to this call.
     */
    private fun tryOpenBrowser(): Boolean {
        if (mIsBrowserOpened) return false
        val url = intent.getStringExtra(KEY_AUTHORIZATION_URL) ?: return false
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
        mIsBrowserOpened = true
        return true
    }
}
