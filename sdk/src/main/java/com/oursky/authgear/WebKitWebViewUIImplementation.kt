package com.oursky.authgear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WebKitWebViewUIImplementation(val actionBarBackgroundColor: Int? = null, val actionBarButtonTintColor: Int? = null) : UIImplementation {

    override fun openAuthorizationURL(
        context: Context,
        options: OpenAuthorizationURLOptions,
        listener: OnOpenAuthorizationURLListener
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val resultURLString =
                    this@WebKitWebViewUIImplementation.openAuthorizationURL(context, options)
                listener.onSuccess(Uri.parse(resultURLString))
            } catch (e: Throwable) {
                listener.onFailed(e)
            }
        }
    }

    private suspend fun openAuthorizationURL(
        context: Context,
        options: OpenAuthorizationURLOptions
    ): String {
        return suspendCoroutine { k ->
            val action = newRandomAction()
            val intentFilter = IntentFilter(action)
            val br =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val type =
                            intent?.getStringExtra(AuthgearCore.KEY_OAUTH_BOARDCAST_TYPE) ?: return
                        when (type) {
                            OAuthBroadcastType.REDIRECT_URL.name -> {
                                context!!.unregisterReceiver(this)
                                val output = intent.getStringExtra(AuthgearCore.KEY_REDIRECT_URL)
                                if (output != null) {
                                    k.resume(output)
                                } else {
                                    k.resumeWithException(CancelException())
                                }
                            }
                        }
                    }
                }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(br, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(br, intentFilter)
            }
            val webViewOptions = WebKitWebViewActivity.Options(options.url, options.redirectURI)
            webViewOptions.actionBarBackgroundColor = this@WebKitWebViewUIImplementation.actionBarBackgroundColor
            webViewOptions.actionBarButtonTintColor = this@WebKitWebViewUIImplementation.actionBarButtonTintColor
            context.startActivity(
                WebKitWebViewActivity.createIntent(
                    context,
                    action,
                    webViewOptions
                )
            )
        }
    }
}
