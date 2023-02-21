package com.oursky.authgear.latte

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.oursky.authgear.*
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalAuthgearApi::class)
object Latte {
    private data class LatteResult(val broadcastAction: String, val finishUri: String?) {
        inline fun <T> handle(authgear: Authgear, fn: (finishUri: String) -> T): LatteHandle<T> {
            val finishUri = this.finishUri ?: return LatteHandle.Failure(authgear, broadcastAction, CancelException())
            return try {
                val value = fn(finishUri)
                LatteHandle.Success(authgear, broadcastAction, value)
            } catch (e: Throwable) {
                LatteHandle.Failure(authgear, broadcastAction, e)
            }
        }
    }

    private fun makeRandomAction(app: Application): String {
        val rng = SecureRandom()
        val byteArray = ByteArray(32)
        rng.nextBytes(byteArray)
        val action = base64UrlEncode(byteArray)
        return "${app.packageName}.latte.$action"
    }

    private suspend fun startActivity(authgear: Authgear, url: Uri, redirectUri: String): LatteResult {
        return suspendCoroutine { k ->
            val app = authgear.core.application
            val broadcastAction = makeRandomAction(app)
            val br = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val message = intent?.let { LatteActivity.extract(it) } ?: return

                    when (message) {
                        is LatteMessage.HandleRedirectURI -> {
                            app.unregisterReceiver(this)
                            k.resume(LatteResult(broadcastAction, message.finishUri))
                        }
                        is LatteMessage.OpenEmailClient -> {
                            if (context == null) return
                            val intent = EmailClient.makeEmailClientIntentChooser(
                                context,
                                "Choose Email Client",
                                listOf(EmailClient.GMAIL, EmailClient.OUTLOOK)
                            )
                            context.startActivity(intent)
                        }
                        else -> {}
                    }
                }
            }
            app.registerReceiver(br, IntentFilter(broadcastAction))
            app.startActivity(
                LatteActivity.createIntent(app, url, redirectUri, broadcastAction)
            )
        }
    }

    suspend fun authenticate(authgear: Authgear, options: AuthenticateOptions): LatteHandle<UserInfo> {
        val request = authgear.createAuthenticateRequest(options)
        val result = startActivity(authgear, request.url, request.redirectUri)
        return result.handle(authgear) {
            authgear.finishAuthentication(it, request)
        }
    }

    suspend fun verifyEmail(authgear: Authgear, email: String, entryUrl: String, redirectUri: String): LatteHandle<UserInfo> {
        val verifyEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()
        val url = authgear.generateUrl(verifyEmailUrl.toString())
        val result = startActivity(authgear, url, redirectUri)
        return result.handle(authgear) {
            authgear.fetchUserInfo()
        }
    }
}