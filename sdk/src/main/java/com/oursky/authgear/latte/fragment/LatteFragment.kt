package com.oursky.authgear.latte.fragment

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.oursky.authgear.CancelException
import com.oursky.authgear.R
import com.oursky.authgear.ServerException
import com.oursky.authgear.latte.*
import com.oursky.authgear.latte.WebView
import com.oursky.authgear.latte.WebViewEvent
import com.oursky.authgear.latte.WebViewListener
import com.oursky.authgear.latte.WebViewRequest
import com.oursky.authgear.latte.WebViewResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.launch
import org.json.JSONObject

internal abstract class LatteFragment<T>() : Fragment(), LatteHandle<T> {
    companion object {
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"
        private const val KEY_REDIRECT_URI = "redirect_uri"
    }

    internal var latte: Latte? = null

    val latteID: String
        get() = requireArguments().getString(KEY_ID)!!

    val url: Uri
        get() = Uri.parse(requireArguments().getString(KEY_URL)!!)

    val redirectUri: String
        get() = requireArguments().getString(KEY_REDIRECT_URI)!!

    internal constructor(id: String, url: Uri, redirectUri: String) : this() {
        arguments = Bundle().apply {
            putString(KEY_ID, id)
            putString(KEY_URL, url.toString())
            putString(KEY_REDIRECT_URI, redirectUri)
        }
    }

    private lateinit var webView: WebView
    private val result = CompletableDeferred<T>()

    private class LatteWebViewListener<T>(val fragment: LatteFragment<T>) : WebViewListener {
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

        override fun onCompleted(result: WebViewResult) {
            fragment.handleFinishUri(result.finishUri)
        }
    }

    private class LatteBackPressHandler<T>(val fragment: LatteFragment<T>) : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (fragment.webView.canGoBack()) {
                fragment.webView.goBack()
            } else {
                fragment.handleFinishUri(null)
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
        webView = WebView(requireContext())
        webView.setBackgroundColor(Color.TRANSPARENT)
        return FrameLayout(requireContext(), null, 0, R.style.LatteFragmentTheme).apply {
            addView(webView)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        webView.request = WebViewRequest(url = url, redirectUri = redirectUri)
        webView.listener = LatteWebViewListener(this)
        webView.load()
    }

    private fun handleFinishUri(finishUri: Uri?) {
        lifecycleScope.launch {
            result.completeWith(runCatching {
                finishUri?.let {
                    val error = it.getQueryParameter("error")
                    if (error == "cancel") {
                        throw CancelException()
                    }

                    val latteError = it.getQueryParameter("x_latte_error")
                    latteError?.let { base64Json ->
                        val json = Base64.decode(base64Json, Base64.URL_SAFE).toString(Charsets.UTF_8)
                        throw ServerException(JSONObject(json))
                    }

                    onHandleFinishUri(it)
                } ?: throw CancelException()
            })
        }
    }

    internal abstract suspend fun onHandleFinishUri(finishUri: Uri): T

    override val id: String
        get() = latteID

    override val fragment: Fragment
        get() = this

    override suspend fun await() = result.await()
}