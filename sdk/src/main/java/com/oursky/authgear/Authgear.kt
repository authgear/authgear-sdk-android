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
    var onRefreshTokenExpiredListener: OnRefreshTokenExpiredListener?
        get() {
            return core.onRefreshTokenExpiredListener
        }
        set(value) {
            core.onRefreshTokenExpiredListener = value
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
                handler.run {
                    onAuthorizeListener.onAuthorized(result)
                }
            } catch (e: Throwable) {
                Log.d(TAG, "$e")
                handler.run {
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
                handler.run {
                    onConfigureListener.onConfigured()
                }
            } catch (e: Throwable) {
                Log.d(TAG, "$e")
                handler.run {
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
