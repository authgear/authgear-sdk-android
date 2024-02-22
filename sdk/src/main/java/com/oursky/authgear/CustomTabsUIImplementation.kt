package com.oursky.authgear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomTabsUIImplementation : UIImplementation {
    override fun openAuthorizationURL(
        context: Context,
        options: OpenAuthorizationURLOptions,
        listener: OnOpenAuthorizationURLListener
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val resultURLString =
                    this@CustomTabsUIImplementation.openAuthorizationURL(context, options)
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
            context.registerReceiver(br, intentFilter)
            context.startActivity(
                OAuthActivity.createAuthorizationIntent(
                    context,
                    action,
                    options.redirectURI.toString(),
                    options.url.toString()
                )
            )
        }
    }
}
