package com.oursky.authgear.latte.fragment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.serialization.encodeToString
import com.oursky.authgear.CancelException
import com.oursky.authgear.R
import com.oursky.authgear.latte.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class LatteFragment() : Fragment() {
    companion object {
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_REDIRECT_URI = "redirect_uri"
        private const val KEY_WEBSITE_INSTPECTABLE = "webview_inspectable"
        private const val KEY_WEBVIEW_STATE = "webview_state"

        internal const val INTENT_KEY_TYPE = "type"
        internal const val INTENT_KEY_EVENT = "event"
        internal const val INTENT_KEY_RESULT = "result"

        internal const val INTENT_RESULT_OK = "ok"

        fun makeWithPreCreatedWebView(
            context: Context,
            id: String,
            url: Uri,
            redirectUri: String,
            webContentsDebuggingEnabled: Boolean
        ): LatteFragment {
            val fragment = LatteFragment()
            fragment.arguments = Bundle().apply {
                putString(KEY_ID, id)
                putString(KEY_URL, url.toString())
                putString(KEY_REDIRECT_URI, redirectUri)
                putBoolean(KEY_WEBSITE_INSTPECTABLE, webContentsDebuggingEnabled)
            }
            fragment.preCreateWebView(context)
            return fragment
        }
    }

    internal enum class BroadcastType {
        COMPLETE,
        OPEN_EMAIL_CLIENT,
        OPEN_SMS_CLIENT,
        TRACKING,
        REAUTH_WITH_BIOMETRIC,
        RESET_PASSWORD_COMPLETED
    }

    val latteID: String
        get() = requireArguments().getString(KEY_ID)!!

    val url: Uri
        get() = Uri.parse(requireArguments().getString(KEY_URL)!!)

    val redirectUri: String
        get() = requireArguments().getString(KEY_REDIRECT_URI)!!

    private val webContentsDebuggingEnabled: Boolean
        get() = requireArguments().getBoolean(KEY_WEBSITE_INSTPECTABLE)

    private var mutWebView: WebView? = null
    private var webView: WebView
        get() = mutWebView!!
        set(value) { mutWebView = value }

    private var webViewOnReady: (() -> Unit)? = null
    private var webViewOnComplete: ((result: Result<WebViewResult>) -> Unit)? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }
            when (intent.action) {
                Latte.BroadcastType.RESET_PASSWORD_COMPLETED.action -> {
                    mutWebView?.dispatchSignal(WebViewSignal.RESET_PASSWORD_COMPLETED)
                }
                else -> {
                    // Ignore
                }
            }
        }
    }

    private class LatteWebViewListener(val fragment: LatteFragment) : WebViewListener {
        override fun onEvent(event: WebViewEvent) {
            when (event) {
                is WebViewEvent.OpenEmailClient -> {
                    fragment.broadcastOnOpenEmailClientIntent()
                }
                is WebViewEvent.OpenSMSClient -> {
                    fragment.broadcastOnOpenSMSClientIntent()
                }
                is WebViewEvent.Tracking -> {
                    fragment.broadcastTrackingIntent(event.event)
                }
                is WebViewEvent.ReauthWithBiometric -> {
                    fragment.broadcastOnReauthWithBiometricIntent()
                }
                is WebViewEvent.ResetPasswordCompleted -> {
                    fragment.broadcastOnResetPasswordCompletedIntent()
                }
            }
        }

        override fun onReady(webView: WebView) {
            fragment.webViewOnReady?.invoke()
        }

        override fun onComplete(webView: WebView, result: Result<WebViewResult>) {
            fragment.broadcastCompleteIntent(result)
            fragment.webViewOnComplete?.invoke(result)
        }
    }

    internal suspend fun waitWebViewToLoad(timeout: Long) = coroutineScope {
        suspendCoroutine<Unit> { k ->
            var isResumed = false
            lateinit var cleanup: (Boolean) -> Unit

            val webViewOnReady: (() -> Unit) = fun() {
                if (isResumed) {
                    return
                }
                isResumed = true
                cleanup(true)
                k.resume(Unit)
            }
            val webViewOnComplete: ((result: Result<WebViewResult>) -> Unit) = fun(result) {
                if (isResumed) {
                    return
                }
                isResumed = true
                cleanup(true)
                k.resumeWith(result.mapCatching {
                    // Throw error if needed
                    LatteResult(
                        finishUri = it.finishUri.toString(),
                        errorMessage = null
                    ).getOrThrow()
                    return
                })
            }

            val timer = async {
                delay(timeout)
                if (isResumed) {
                    return@async
                }
                isResumed = true
                cleanup(false)
                k.resumeWith(Result.failure(LatteException.Timeout))
            }
            cleanup = fun(cancelTimer: Boolean) {
                if (this@LatteFragment.webViewOnReady == webViewOnReady) {
                    this@LatteFragment.webViewOnReady = null
                }
                if (this@LatteFragment.webViewOnComplete == webViewOnComplete) {
                    this@LatteFragment.webViewOnComplete = null
                }
                if (cancelTimer && timer.isActive) {
                    timer.cancel()
                }
            }
            this@LatteFragment.webViewOnReady = webViewOnReady
            this@LatteFragment.webViewOnComplete = webViewOnComplete
            webView.load()
        }
    }

    private fun broadcastOnOpenEmailClientIntent() {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.OPEN_EMAIL_CLIENT.toString())
        ctx.sendOrderedBroadcast(broadcastIntent, null)
    }

    private fun broadcastOnOpenSMSClientIntent() {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.OPEN_SMS_CLIENT.toString())
        ctx.sendOrderedBroadcast(broadcastIntent, null)
    }

    private fun broadcastOnReauthWithBiometricIntent() {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.REAUTH_WITH_BIOMETRIC.toString())
        ctx.sendOrderedBroadcast(broadcastIntent, null)
    }

    private fun broadcastOnResetPasswordCompletedIntent() {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.RESET_PASSWORD_COMPLETED.toString())
        ctx.sendOrderedBroadcast(broadcastIntent, null)
    }

    private fun broadcastTrackingIntent(event: LatteTrackingEvent) {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.TRACKING.toString())
        broadcastIntent.putExtra(INTENT_KEY_EVENT, Json.encodeToString(event))
        ctx.sendOrderedBroadcast(broadcastIntent, null)
    }

    private fun broadcastCompleteIntent(result: Result<WebViewResult>) {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.COMPLETE.toString())
        val latteResult: LatteResult = try {
            val webViewResult = result.getOrThrow()
            LatteResult(finishUri = webViewResult.finishUri.toString())
        } catch (e: CancelException) {
            LatteResult(isCancel = true)
        } catch (e: Throwable) {
            LatteResult(errorMessage = e.message)
        }
        broadcastIntent.putExtra(INTENT_KEY_RESULT, Json.encodeToString(latteResult))
        val br = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                // Intent was handled, we don't have to do anything
                if (resultData == INTENT_RESULT_OK) {
                    return
                }
                // Else, pop the current fragment
                if (!isAdded) {
                    return
                }
                parentFragmentManager.popBackStack(latteID, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }
        ctx.sendOrderedBroadcast(
            broadcastIntent,
            null,
            br,
            null,
            Activity.RESULT_OK,
            null,
            null
        )
    }

    private fun constructWebViewIfNeeded(ctx: Context, stateBundle: Bundle?) {
        if (mutWebView != null) {
            return
        }

        val newWebView = WebView(ctx, WebViewRequest(url = url, redirectUri = redirectUri), webContentsDebuggingEnabled)
        mutWebView = newWebView
        newWebView.setBackgroundColor(Color.TRANSPARENT)
        newWebView.listener = LatteWebViewListener(this)
        if (stateBundle != null) {
            newWebView.restoreState(stateBundle)
        }
    }

    private class LatteBackPressHandler(val fragment: LatteFragment) : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (fragment.webView.canGoBack()) {
                fragment.webView.goBack()
            } else {
                fragment.broadcastCompleteIntent(Result.failure(CancelException()))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backDispatcher = requireActivity().onBackPressedDispatcher
        backDispatcher.addCallback(this, LatteBackPressHandler(this))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val webViewStateBundle = savedInstanceState?.getBundle(KEY_WEBVIEW_STATE)
        val ctx = requireContext()
        constructWebViewIfNeeded(ctx, webViewStateBundle)
        removeWebViewFromParent(webView)
        val intentFilter = IntentFilter(Latte.BroadcastType.RESET_PASSWORD_COMPLETED.action)
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(broadcastReceiver, intentFilter)
        }

        return FrameLayout(requireContext(), null, 0, R.style.LatteFragmentTheme).apply {
            addView(webView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeWebViewFromParent(webView)
        mutWebView = null
        val ctx = requireContext()
        ctx.unregisterReceiver(broadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webView = mutWebView ?: return
        val webViewState = Bundle()
        webView.saveState(webViewState)
        outState.putBundle(KEY_WEBVIEW_STATE, webViewState)
    }

    private fun removeWebViewFromParent(webView: WebView) {
        val webViewParent = webView.parent
        if (webViewParent != null) {
            (webViewParent as? ViewGroup)?.removeView(webView)
        }
    }

    private fun preCreateWebView(ctx: Context) {
        constructWebViewIfNeeded(ctx, null)
    }

    fun listen(ctx: Context, latte: Latte): ListenHandle {
        // This method setup broadcast receiver for the fragment but do not block the code
        val intentFilter = IntentFilter(latteID)
        var handle: ListenHandle? = null
        val br = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val type = intent?.getStringExtra(INTENT_KEY_TYPE) ?: return
                resultData = INTENT_RESULT_OK
                when (type) {
                    BroadcastType.OPEN_EMAIL_CLIENT.name -> {
                        latte.delegate?.onOpenEmailClient(context)
                    }
                    BroadcastType.OPEN_SMS_CLIENT.name -> {
                        latte.delegate?.onOpenSMSClient(context)
                    }
                    BroadcastType.TRACKING.name -> {
                        val eventStr = intent.getStringExtra(INTENT_KEY_EVENT) ?: return
                        val event = Json.decodeFromString<LatteTrackingEvent>(eventStr)
                        latte.delegate?.onTrackingEvent(event)
                    }
                    BroadcastType.COMPLETE.name -> {
                        val resultStr = intent.getStringExtra(INTENT_KEY_RESULT) ?: return
                        val result = Json.decodeFromString<LatteResult>(resultStr)
                        handle?.onComplete?.invoke(result)
                    }
                    BroadcastType.REAUTH_WITH_BIOMETRIC.name -> {
                        handle?.onReauthWithBiometric?.invoke()
                    }
                    BroadcastType.RESET_PASSWORD_COMPLETED.name -> {
                        handle?.onResetPasswordCompleted?.invoke()
                    }
                }
            }
        }
        handle = ListenHandle(ctx, br)
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(br, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(br, intentFilter)
        }
        return handle
    }

    class ListenHandle(private val ctx: Context, private val br: BroadcastReceiver) {
        var onComplete: ((LatteResult) -> Unit)? = null
        var onReauthWithBiometric: (() -> Unit)? = null
        var onResetPasswordCompleted: (() -> Unit)? = null

        suspend fun <T> waitForResult(
            listener: LatteFragmentListener<T>?,
            callback: (LatteResult, (Result<T>) -> Unit) -> Unit
        ): T {
            val result: T = suspendCoroutine<T> { k ->
                var isResumed = false
                val resumeWith = fun(result: Result<T>) {
                    if (isResumed) {
                        return
                    }
                    isResumed = true
                    k.resumeWith(result)
                }
                onReauthWithBiometric = {
                    listener?.onReauthWithBiometric(resumeWith)
                }
                onResetPasswordCompleted = {
                    listener?.onResetPasswordCompleted(resumeWith)
                }
                onComplete = fun(latteResult: LatteResult) {
                    if (isResumed) {
                        return
                    }
                    try {
                        callback(latteResult, resumeWith)
                    } catch (e: Throwable) {
                        resumeWith(Result.failure(e))
                    }
                    ctx.unregisterReceiver(br)
                }
            }
            return result
        }
    }
}
