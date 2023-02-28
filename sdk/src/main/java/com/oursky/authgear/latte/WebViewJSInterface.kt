package com.oursky.authgear.latte

import android.webkit.JavascriptInterface
import kotlinx.serialization.json.*

internal class WebViewJSInterface(private val webView: WebView) {
    companion object {
        const val jsBridgeName = "latte"
        const val initScript = """
            if (!window.__latteInitialized) {
                document.addEventListener('latte:event',
                    function (e) {
                        window.latte.handleEvent(JSON.stringify(e.detail));
                    }
                )
                window.__latteInitialized = true
            }
        """
    }

    enum class BuiltInEvent(val eventName: String) {
        OPEN_EMAIL_CLIENT("openEmailClient"),
        VIEW_PAGE("viewPage")
    }

    @JavascriptInterface
    fun handleEvent(eventJSON: String) {
        val event = Json.parseToJsonElement(eventJSON)
        val type = try {
            event.jsonObject["type"]?.jsonPrimitive?.contentOrNull?.let { name ->
                BuiltInEvent.values().find { it.eventName == name }
            }
        } catch (e: Exception) { null } ?: return

        when (type) {
            BuiltInEvent.OPEN_EMAIL_CLIENT -> this.webView.listener?.onEvent(WebViewEvent.OpenEmailClient)
            BuiltInEvent.VIEW_PAGE -> {
                val path = try {
                    event.jsonObject["path"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: return
                this.webView.listener?.onEvent(WebViewEvent.ViewPage(LatteViewPageEvent(path)))
            }
        }
    }
}