package com.oursky.authgear.latte.fragment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
        TRACKING
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

    private class LatteWebViewListener(val fragment: LatteFragment) : WebViewListener {
        override fun onEvent(event: WebViewEvent) {
            when (event) {
                is WebViewEvent.OpenEmailClient -> {
                    fragment.broadcastOnOpenEmailClientIntent()
                }
                is WebViewEvent.Tracking -> {
                    fragment.broadcastTrackingIntent(event.event)
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

    internal suspend fun waitWebViewToLoad() {
        suspendCoroutine<Unit> { k ->
            var isResumed = false
            lateinit var cleanup: () -> Unit
            val webViewOnReady: (() -> Unit) = fun() {
                if (isResumed) {
                    return
                }
                isResumed = true
                cleanup()
                k.resume(Unit)
            }
            val webViewOnComplete: ((result: Result<WebViewResult>) -> Unit) = fun(result) {
                if (isResumed) {
                    return
                }
                isResumed = true
                cleanup()
                k.resumeWith(result.mapCatching {
                    // Throw error if needed
                    LatteResult(
                        finishUri = it.finishUri.toString(),
                        errorMessage = null
                    ).getOrThrow()
                    return
                })
            }
            cleanup = fun() {
                if (this.webViewOnReady == webViewOnReady) {
                    this.webViewOnReady = null
                }
                if (this.webViewOnComplete == webViewOnComplete) {
                    this.webViewOnComplete = null
                }
            }
            this.webViewOnReady = webViewOnReady
            this.webViewOnComplete = webViewOnComplete
            webView.load()
        }
    }

    private fun broadcastOnOpenEmailClientIntent() {
        val ctx = context ?: return
        val broadcastIntent = Intent(latteID)
        broadcastIntent.putExtra(INTENT_KEY_TYPE, BroadcastType.OPEN_EMAIL_CLIENT.toString())
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
        constructWebViewIfNeeded(requireContext(), webViewStateBundle)
        removeWebViewFromParent(webView)
        return FrameLayout(requireContext(), null, 0, R.style.LatteFragmentTheme).apply {
            addView(webView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeWebViewFromParent(webView)
        mutWebView = null
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
}