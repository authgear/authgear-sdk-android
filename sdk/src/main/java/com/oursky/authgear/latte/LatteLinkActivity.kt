package com.oursky.authgear.latte

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LatteLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLink = intent.data
        if (appLink == null) {
            finish()
            return
        }
        val intent = LatteLink.createLinkReceivedIntent(appLink, application)
        application.sendBroadcast(intent)
        finish()
    }
}