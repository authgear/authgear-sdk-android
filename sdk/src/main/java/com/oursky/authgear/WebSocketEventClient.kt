package com.oursky.authgear

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

internal class WebSocketEventClient(
    private val authgearEndpoint: String
) : WebSocketClient.EventListener {
    companion object {
        val json = Json { ignoreUnknownKeys = true }
        private val TAG = WebSocketEventClient::class.java.simpleName
    }

    var listener: EventListener? = null
    private var webSocket: WebSocketClient? = null
    private var wsChannelID: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(channelID: String) {
        scope.launch {
            doConnect(channelID)
        }
    }

    fun disconnect() {
        scope.launch {
            dispose()
        }
    }

    private fun doConnect(channelID: String) {
        wsChannelID = channelID
        if (webSocket != null) {
            dispose()
        }
        val u = URI(authgearEndpoint)
        val scheme = if (u.scheme == "https") "wss" else "ws"
        val wsURI = URI(
            scheme, u.authority,
            "/ws", "x_ws_channel_id=%s".format(wsChannelID),
            null
        )
        webSocket = WebSocketClient(wsURI, this)
        webSocket?.connect()
    }

    private fun dispose() {
        // remove listener, so we won't recieve websocket event and won't reconnect in onClose
        webSocket?.listener = null
        webSocket?.close()
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (code != 1000) {
            scope.launch {
                webSocket?.reconnect()
            }
        }
    }

    override fun onMessage(message: String?) {
        message?.let {
            try {
                val decoded: WebSocketMessage = json.decodeFromString(it)
                listener?.OnMessage(decoded.kind.raw, decoded.data)
            } catch (e: Exception) {
                Log.e(TAG, "failed to decode websocket event", e)
            }
        }
    }

    override fun onError(ex: Exception?) {
    }

    interface EventListener {
        fun OnMessage(eventKind: String, data: JsonObject?)
    }
}