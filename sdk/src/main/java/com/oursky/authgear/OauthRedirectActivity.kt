package com.oursky.authgear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * An activity to be declared by user in their app's manifest to handle redirect deep link.
 */
class OauthRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deepLink = intent.data?.toString()
        if (deepLink != null && AuthgearCore.handleWeChatRedirectDeepLink(deepLink)) {
            // this is wechat callback skip the other handling
            finish()
            return
        }
        startActivity(OauthActivity.createHandleDeepLinkIntent(this, intent.data))
        finish()
    }
}
