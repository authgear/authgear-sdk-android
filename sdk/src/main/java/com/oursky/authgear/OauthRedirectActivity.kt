package com.oursky.authgear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * An activity to be declared by user in their app's manifest to handle redirect deep link.
 */
class OauthRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(OauthActivity.createHandleDeepLinkIntent(this, intent.data))
        finish()
    }
}
