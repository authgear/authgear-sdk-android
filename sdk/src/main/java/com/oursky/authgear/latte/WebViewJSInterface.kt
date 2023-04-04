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
        TRACKING("tracking")
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
            BuiltInEvent.TRACKING -> {
                val event_name = try {
                    event.jsonObject["event_name"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: return
                val params = try {
                    event.jsonObject["params"]?.jsonObject
                } catch (e: Exception) { null } ?: return
                this.webView.listener?.onEvent(WebViewEvent.Tracking(LatteTrackingEvent(event_name, params)))
            }
        }
    }
}