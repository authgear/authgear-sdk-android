package com.oursky.authgear

import android.util.Log
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

internal class WebSocketClient : org.java_websocket.client.WebSocketClient {
    companion object {
        private val TAG = WebSocketClient::class.java.simpleName
    }

    var listener: EventListener? = null

    constructor(
        uri: URI,
        eventListener: EventListener
    ) : super(uri) {
        this.listener = eventListener
        val scheme: String = this.uri.getScheme()
        if (scheme != null && scheme.equals("wss", ignoreCase = true)) {
            val keyManagers: Array<KeyManager>
            keyManagers = try {
                val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                val keyManagerFactory: KeyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                keyManagerFactory.init(keyStore, null)
                keyManagerFactory.getKeyManagers()
            } catch (e: Throwable) {
                Log.e(TAG, "failed to create SSL Key Managers", e)
                throw e
            }
            val ctx: SSLContext
            try {
                ctx = SSLContext.getInstance("TLS")
                ctx.init(keyManagers, null, SecureRandom())
                val factory: SSLSocketFactory = ctx.getSocketFactory()
                setSocketFactory(factory)
            } catch (e: Throwable) {
                Log.e(TAG, "failed to create SSL Context", e)
                throw e
            }
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d(TAG, "ws onOpen: %d".format(handshakedata?.httpStatus))
        listener?.onOpen(handshakedata)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "ws onClose: %d, %s, %b".format(code, reason, remote))
        listener?.onClose(code, reason, remote)
    }

    override fun onMessage(message: String?) {
        Log.d(TAG, "ws onMessage")
        listener?.onMessage(message)
    }

    override fun onError(ex: Exception?) {
        Log.e(TAG, "ws onError", ex)
        listener?.onError(ex)
    }

    interface EventListener {
        fun onError(ex: Exception?)
        fun onMessage(message: String?)
        fun onOpen(handshakedata: ServerHandshake?)
        fun onClose(code: Int, reason: String?, remote: Boolean)
    }
}
