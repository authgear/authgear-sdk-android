package com.oursky.authgear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

internal class CustomTabActivity : AppCompatActivity() {
    companion object {
        @Suppress("unused")
        private val TAG = CustomTabActivity::class.java.simpleName
        private const val KEY_URL = "url"
        /**
         * Create an intent to open a Chrome custom tab
         */
        fun createCustomTabIntent(context: Context, url: String): Intent {
            val intent = Intent(context, CustomTabActivity::class.java)
            intent.putExtra(CustomTabActivity.KEY_URL, url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
        // Open the custom tab if not yet opened
        if (tryOpenBrowser()) return
        finish()
    }
    /**
     * @return `true` if a browser is opened due to this call.
     */
    private fun tryOpenBrowser(): Boolean {
        if (mIsBrowserOpened) return false
        val url = intent.getStringExtra(KEY_URL) ?: return false
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
        mIsBrowserOpened = true
        return true
    }
}