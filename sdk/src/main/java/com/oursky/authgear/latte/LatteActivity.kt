package com.oursky.authgear.latte

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class LatteActivity : AppCompatActivity(), WebViewListener {
    companion object {
        const val KEY_URL = "url"
        const val KEY_REDIRECT_URI = "redirect_uri"
        const val KEY_BROADCAST_ACTION = "broadcast_action"

        const val KEY_MESSAGE = "message"

        fun createIntent(app: Application, url: Uri, redirectUri: String, broadcastAction: String): Intent {
            return Intent(app, LatteActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(KEY_URL, url.toString())
                putExtra(KEY_REDIRECT_URI, redirectUri)
                putExtra(KEY_BROADCAST_ACTION, broadcastAction)
            }
        }

        fun extract(intent: Intent): LatteMessage? = intent.getStringExtra(KEY_MESSAGE)?.let {
            Json.decodeFromString<LatteMessage>(it)
        }

        fun dispatch(message: LatteMessage, app: Application, broadcastAction: String) {
            val intent = Intent(broadcastAction).apply {
                setPackage(app.packageName)
                putExtra(KEY_MESSAGE, Json.encodeToString(message))
            }
            app.sendBroadcast(intent)
        }
    }

    private lateinit var webView: WebView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent ?.let { extract(it) } ?: return

            when (message) {
                is LatteMessage.Finish -> this@LatteActivity.finish()
                else -> {}
            }
        }
    }

    override fun finish() {
        dispatch(LatteMessage.HandleRedirectURI(null))
        super.finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val broadcastAction = intent.getStringExtra(KEY_BROADCAST_ACTION)!!
        registerReceiver(receiver, IntentFilter(broadcastAction))

        webView = WebView(this)
        setContentView(webView)

        webView.request = WebViewRequest(
            url = Uri.parse(intent.getStringExtra(KEY_URL)!!),
            redirectUri = intent.getStringExtra(KEY_REDIRECT_URI)!!
        )
        webView.listener = this
        webView.load()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onEvent(event: WebViewEvent) {
        when (event) {
            is WebViewEvent.OpenEmailClient -> dispatch(LatteMessage.OpenEmailClient)
            is WebViewEvent.ViewPage -> dispatch(LatteMessage.ViewPage(event.event))
        }
    }

    override fun onCompleted(result: WebViewResult) {
        dispatch(LatteMessage.HandleRedirectURI(result.finishUri.toString()))
    }

    private fun dispatch(message: LatteMessage) {
        val broadcastAction = intent.getStringExtra(KEY_BROADCAST_ACTION)!!
        dispatch(message, application, broadcastAction)
    }
}