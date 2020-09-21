package com.oursky.authgear

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.oursky.authgear.data.oauth.OauthRepoHttp
import com.oursky.authgear.data.token.TokenRepoEncryptedSharedPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Authgear @JvmOverloads constructor(application: Application, name: String? = null) {
    companion object {
        @Suppress("unused")
        private val TAG = Authgear::class.java.simpleName
    }

    private val core =
        AuthgearCore(application, TokenRepoEncryptedSharedPref(application), OauthRepoHttp(), name)
    private val scope = CoroutineScope(Dispatchers.IO)
    val clientId: String?
        get() = core.clientId
    val onRefreshTokenExpiredListener: OnRefreshTokenExpiredListener?
        get() {
            return core.onRefreshTokenExpiredListener?.listener
        }
    val sessionState: SessionState
        get() {
            return core.sessionState
        }

    @JvmOverloads
    fun setOnRefreshTokenExpiredListener(
        listener: OnRefreshTokenExpiredListener,
        handler: Handler = Handler(
                    Looper.getMainLooper()
                )
    ) {
        core.onRefreshTokenExpiredListener = ListenerPair(listener, handler)
    }

    fun authenticateAnonymously() {
        scope.launch {
            core.authenticateAnonymously()
        }
    }

    @JvmOverloads
    fun authorize(
        options: AuthorizeOptions,
        onAuthorizeListener: OnAuthorizeListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                val result = core.authorize(options)
                handler.post {
                    onAuthorizeListener.onAuthorized(result)
                }
            } catch (e: Throwable) {
                Log.d(TAG, "$e")
                handler.post {
                    onAuthorizeListener.onAuthorizationFailed(e)
                }
            }
        }
    }

    @JvmOverloads
    fun configure(
        options: ConfigureOptions,
        onConfigureListener: OnConfigureListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.configure(options)
                handler.post {
                    onConfigureListener.onConfigured()
                }
            } catch (e: Throwable) {
                Log.d(TAG, "$e")
                handler.post {
                    onConfigureListener.onConfigurationFailed(e)
                }
            }
        }
    }

    fun logout() {
        scope.launch {
            core.logout()
        }
    }

    fun handleDeepLink() {
        scope.launch {
            core.handleDeepLink()
        }
    }

    fun promoteAnonymousUser() {
        scope.launch {
            core.promoteAnonymousUser()
        }
    }
}
