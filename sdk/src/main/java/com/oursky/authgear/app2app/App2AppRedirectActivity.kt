package com.oursky.authgear.app2app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class App2AppRedirectActivity : AppCompatActivity() {
    companion object {
        const val BROADCAST_ACTION = "com.oursky.authgear.app2app.App2AppRedirectActivity.action"
        const val KEY_REDIRECT_URL = "redirectUrl"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val broadcastIntent = Intent(BROADCAST_ACTION)
        broadcastIntent.setPackage(packageName)
        intent.data?.let {
            broadcastIntent.putExtra(KEY_REDIRECT_URL, it.toString())
            this.sendBroadcast(broadcastIntent)
        }
        finish()
    }
}
