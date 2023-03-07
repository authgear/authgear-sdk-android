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
        ANALYTICS("analytics")
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
            BuiltInEvent.ANALYTICS -> {
                val type = try {
                    event.jsonObject["analytics_event_type"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: return
                val path = try {
                    event.jsonObject["path"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: ""
                val url = try {
                    event.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: ""
                val clientID = try {
                    event.jsonObject["client_id"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: ""
                val data = try {
                    event.jsonObject["data"]?.jsonObject
                } catch (e: Exception) { null }
                this.webView.listener?.onEvent(WebViewEvent.Analytics(LatteAnalyticsEvent(type, path, url, clientID, data)))
            }
        }
    }
}