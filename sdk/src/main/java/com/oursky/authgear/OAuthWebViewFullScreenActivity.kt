package com.oursky.authgear

import android.content.Context
import android.content.Intent
import android.net.Uri

internal class OAuthWebViewFullScreenActivity : OAuthWebViewBaseActivity() {

    companion object {
        fun createIntent(ctx: Context?, broadcastAction: String, uri: Uri?, redirectUri: Uri?): Intent {
            val intent = Intent(ctx, OAuthWebViewFullScreenActivity::class.java)
            intent.data = uri
            intent.putExtra(OAuthWebViewBaseActivity.KEY_BROADCAST_ACTION, broadcastAction)
            intent.putExtra(OAuthWebViewBaseActivity.KEY_REDIRECT_URI, redirectUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }
    }
}
