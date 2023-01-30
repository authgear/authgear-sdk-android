package com.oursky.authgear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

internal class OAuthActivity : AppCompatActivity() {
    companion object {
        @Suppress("unused")
        private val TAG = OAuthActivity::class.java.simpleName
        private const val KEY_REDIRECT_URL = "redirectUrl"
        private const val KEY_AUTHORIZATION_URL = "authorizationUrl"
        private const val KEY_BROADCAST_ACTION = "broadcastAction"
        /**
         * Create an intent to open a browser to perform login.
         */
        fun createAuthorizationIntent(context: Context, broadcastAction: String, redirectUrl: String, url: String): Intent {
            val intent = Intent(context, OAuthActivity::class.java)
            intent.putExtra(KEY_BROADCAST_ACTION, broadcastAction)
            intent.putExtra(KEY_REDIRECT_URL, redirectUrl)
            intent.putExtra(KEY_AUTHORIZATION_URL, url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }
        /**
         * Create an intent to handle a deep link launched from browser.
         */
        fun createHandleDeepLinkIntent(context: Context, uri: Uri?): Intent {
            val intent = Intent(context, OAuthActivity::class.java)
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return intent
        }
    }
    private var mIsBrowserOpened = false
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.getIntent().setData(intent?.getData())
        this.intent = this.intent
    }

    override fun onResume() {
        super.onResume()
        // Either open the browser or finish with or without deep link.
        if (tryOpenBrowser()) return

        intent.getStringExtra(KEY_BROADCAST_ACTION)?.let { broadcastAction ->
            val broadcastIntent = Intent(broadcastAction)
            broadcastIntent.putExtra(AuthgearCore.KEY_OAUTH_BOARDCAST_TYPE, OAuthBroadcastType.REDIRECT_URL.name)
            this.intent.data?.toString()?.let {
                broadcastIntent.putExtra(AuthgearCore.KEY_REDIRECT_URL, it)
            }
            this.sendBroadcast(broadcastIntent)
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
