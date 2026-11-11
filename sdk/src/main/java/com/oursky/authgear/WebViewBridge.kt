package com.oursky.authgear

import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URISyntaxException

// The bridge between AuthUI and the app, as window.authgearBridge in the WebView.
// See docs/specs/webview-bridge.md in authgear-server for the messages.
internal class WebViewBridge(private val activity: ComponentActivity) {
    companion object {
        const val OBJECT_NAME = "authgearBridge"
        private const val LOGTAG = "Authgear"
    }

    fun install(webView: WebView, allowedOrigins: List<Uri>) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            return
        }
        val rules = try {
            allowedOrigins.mapNotNull { allowedOriginRule(URI(it.toString())) }.toSet()
        } catch (e: URISyntaxException) {
            Log.w(LOGTAG, "authgearBridge is not installed: invalid URL", e)
            return
        }
        if (rules.isEmpty()) {
            Log.w(LOGTAG, "authgearBridge is not installed: no allowed origin")
            return
        }
        // The WebView rejects a rule it cannot parse. Then the page has no bridge.
        try {
            WebViewCompat.addWebMessageListener(
                webView,
                OBJECT_NAME,
                rules
            ) { _, message, _, isMainFrame, replyProxy ->
                val data = message.data
                if (!isMainFrame || data == null) {
                    return@addWebMessageListener
                }
                val request = parseRequest(data) ?: return@addWebMessageListener
                activity.lifecycleScope.launch {
                    handle(request, replyProxy)
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.w(LOGTAG, "authgearBridge is not installed: invalid origin rule", e)
        }
    }

    private fun handle(request: BridgeRequest, replyProxy: JavaScriptReplyProxy) {
        when (request.type) {
            BridgeMessageType.CAPABILITIES -> replyProxy.postMessage(capabilitiesReply(request.id))
            else -> replyProxy.postMessage(reply(request.id, BridgeStatus.UNSUPPORTED))
        }
    }
}

internal object BridgeMessageType {
    const val CAPABILITIES = "bridge.capabilities"
}

internal object BridgeStatus {
    const val OK = "ok"
    const val UNSUPPORTED = "unsupported"
    const val ERROR = "error"
}

// The capabilities listed in the reply to bridge.capabilities.
internal val BRIDGE_CAPABILITIES: List<String> = listOf()

internal data class BridgeRequest(
    val id: String,
    val type: String?,
    val json: JsonObject
)

// The rule of WebViewCompat.addWebMessageListener that allows the origin of url,
// or null when url has no scheme or host. The authority is not used because it can carry user info.
internal fun allowedOriginRule(url: URI): String? {
    val scheme = url.scheme ?: return null
    val host = url.host ?: return null
    val rule = "$scheme://$host"
    if (url.port != -1) {
        return "$rule:${url.port}"
    }
    return rule
}

internal fun sameOrigin(a: URI, b: URI): Boolean {
    val schemeA = a.scheme?.lowercase() ?: return false
    val schemeB = b.scheme?.lowercase() ?: return false
    val hostA = a.host?.lowercase() ?: return false
    val hostB = b.host?.lowercase() ?: return false
    return schemeA == schemeB && hostA == hostB && portOf(a, schemeA) == portOf(b, schemeB)
}

private fun portOf(url: URI, scheme: String): Int {
    if (url.port != -1) {
        return url.port
    }
    return when (scheme) {
        "http" -> 80
        "https" -> 443
        else -> -1
    }
}

// Returns null when text is not a JSON object with a string id.
internal fun parseRequest(text: String): BridgeRequest? {
    val json = try {
        Json.parseToJsonElement(text)
    } catch (e: Exception) {
        return null
    }
    if (json !is JsonObject) {
        return null
    }
    val id = json["id"] as? JsonPrimitive ?: return null
    if (!id.isString) {
        return null
    }
    val type = (json["type"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    return BridgeRequest(id.content, type, json)
}

internal fun reply(
    id: String,
    status: String,
    message: String? = null,
    fields: Map<String, JsonElement> = mapOf()
): String {
    val obj = buildJsonObject {
        put("id", id)
        put("status", status)
        if (message != null) {
            put("message", message)
        }
        fields.forEach { (key, value) -> put(key, value) }
    }
    return obj.toString()
}

internal fun capabilitiesReply(id: String): String {
    return reply(
        id,
        BridgeStatus.OK,
        fields = mapOf("capabilities" to JsonArray(BRIDGE_CAPABILITIES.map { JsonPrimitive(it) }))
    )
}
