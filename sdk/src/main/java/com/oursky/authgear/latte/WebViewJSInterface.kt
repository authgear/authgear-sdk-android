package com.oursky.authgear.latte

import android.content.Intent
import android.net.Uri
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
                document.addEventListener('click',
                    function (e) {
                        if (e.target.tagName === 'A' && e.target.getAttribute('target') === '_blank') {
                            e.preventDefault();
                            window.latte.handleEvent(JSON.stringify({
                                type: 'openExternalLink',
                                url: e.target.href
                            }));
                            return false;
                        }
                    },
                    true
                )
                window.__latteInitialized = true
            }
        """
    }

    enum class BuiltInEvent(val eventName: String) {
        OPEN_EMAIL_CLIENT("openEmailClient"),
        OPEN_SMS_CLIENT("openSMSClient"),
        OPEN_EXTERNAL_LINK("openExternalLink"),
        TRACKING("tracking"),
        READY("ready"),
        REAUTH_WITH_BIOMETRIC("reauthWithBiometric"),
        RESET_PASSWORD_COMPLETED("resetPasswordCompleted")
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
            BuiltInEvent.OPEN_SMS_CLIENT -> this.webView.listener?.onEvent(WebViewEvent.OpenSMSClient)
            BuiltInEvent.OPEN_EXTERNAL_LINK -> {
                val url = try {
                    event.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: return
                val uri = try {
                    Uri.parse(url)
                } catch (e: Exception) { null } ?: return
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                this.webView.context.startActivity(browserIntent)
            }
            BuiltInEvent.TRACKING -> {
                val event_name = try {
                    event.jsonObject["event_name"]?.jsonPrimitive?.contentOrNull
                } catch (e: Exception) { null } ?: return
                val params = try {
                    event.jsonObject["params"]?.jsonObject
                } catch (e: Exception) { null } ?: return
                this.webView.listener?.onEvent(WebViewEvent.Tracking(LatteTrackingEvent(event_name, params)))
            }
            BuiltInEvent.READY -> {
                this.webView.listener?.onReady(this.webView)
            }
            BuiltInEvent.REAUTH_WITH_BIOMETRIC -> {
                this.webView.listener?.onEvent(WebViewEvent.ReauthWithBiometric)
            }
            BuiltInEvent.RESET_PASSWORD_COMPLETED -> {
                this.webView.listener?.onEvent(WebViewEvent.ResetPasswordCompleted)
            }
        }
    }
}
