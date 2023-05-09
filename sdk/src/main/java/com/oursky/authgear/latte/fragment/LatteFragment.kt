package com.oursky.authgear.latte.fragment

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.oursky.authgear.CancelException
import com.oursky.authgear.R
import com.oursky.authgear.latte.*
import java.lang.ref.WeakReference

internal class LatteFragment() : Fragment() {
    companion object {
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_REDIRECT_URI = "redirect_uri"
        private const val KEY_WEBSITE_INSTPECTABLE = "webview_inspectable"
    }

    private var latteRef: WeakReference<Latte?> = WeakReference(null)
    internal var latte: Latte?
        get() = latteRef.get()
        set(value) { latteRef = WeakReference(value) }

    val latteID: String
        get() = requireArguments().getString(KEY_ID)!!

    val url: Uri
        get() = Uri.parse(requireArguments().getString(KEY_URL)!!)

    val redirectUri: String
        get() = requireArguments().getString(KEY_REDIRECT_URI)!!

    private val webContentsDebuggingEnabled: Boolean
        get() = requireArguments().getBoolean(KEY_WEBSITE_INSTPECTABLE)

    internal constructor(context: Context, id: String, url: Uri, redirectUri: String, webContentsDebuggingEnabled: Boolean) : this() {
        arguments = Bundle().apply {
            putString(KEY_ID, id)
            putString(KEY_URL, url.toString())
            putString(KEY_REDIRECT_URI, redirectUri)
            putBoolean(KEY_WEBSITE_INSTPECTABLE, webContentsDebuggingEnabled)
        }
        webView = WebView(context, WebViewRequest(url = url, redirectUri = redirectUri), webContentsDebuggingEnabled)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.listener = LatteWebViewListener(this)
    }

    internal lateinit var webView: WebView

    private class LatteWebViewListener(val fragment: LatteFragment) : WebViewListener {
        override fun onEvent(event: WebViewEvent) {
            when (event) {
                is WebViewEvent.OpenEmailClient -> {
                    val context = fragment.requireContext()
                    val intent = EmailClient.makeEmailClientIntentChooser(
                        context,
                        "Choose Email Client",
                        listOf(EmailClient.GMAIL, EmailClient.OUTLOOK)
                    )
                    context.startActivity(intent)
                }
                is WebViewEvent.Tracking -> {
                    fragment.latte?.delegate?.onTrackingEvent(event.event)
                }
            }
        }
    }

    private class LatteBackPressHandler(val fragment: LatteFragment) : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (fragment.webView.canGoBack()) {
                fragment.webView.goBack()
            } else {
                fragment.webView.completion?.invoke(fragment.webView, Result.failure(CancelException()))
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
    ): View? {
        return FrameLayout(requireContext(), null, 0, R.style.LatteFragmentTheme).apply {
            addView(webView)
        }
    }
}