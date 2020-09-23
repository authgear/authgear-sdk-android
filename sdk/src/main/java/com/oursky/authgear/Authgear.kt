package com.oursky.authgear

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.oursky.authgear.data.key.KeyRepoKeystore
import com.oursky.authgear.data.oauth.OauthRepoHttp
import com.oursky.authgear.data.token.TokenRepoEncryptedSharedPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

class Authgear @JvmOverloads constructor(application: Application, name: String? = null) {
    companion object {
        @Suppress("unused")
        private val TAG = Authgear::class.java.simpleName
    }

    private val core =
        AuthgearCore(
            application,
            TokenRepoEncryptedSharedPref(application),
            OauthRepoHttp(),
            KeyRepoKeystore(),
            name
        )
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

    @JvmOverloads
    fun authenticateAnonymously(
        onAuthenticateAnonymouslyListener: OnAuthenticateAnonymouslyListener,
        handler: Handler = Handler(
            Looper.getMainLooper()
        )
    ) {
        scope.launch {
            try {
                val userInfo = core.authenticateAnonymously()
                handler.post {
                    onAuthenticateAnonymouslyListener.onAuthenticated(userInfo)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    onAuthenticateAnonymouslyListener.onAuthenticationFailed(e)
                }
            }
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
                e.printStackTrace()
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
                e.printStackTrace()
                handler.post {
                    onConfigureListener.onConfigurationFailed(e)
                }
            }
        }
    }

    @JvmOverloads
    fun logout(
        onLogoutListener: OnLogoutListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.logout()
                handler.post {
                    onLogoutListener.onLogout()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    onLogoutListener.onLogoutFailed(e)
                }
            }
        }
    }

    fun handleDeepLink() {
        scope.launch {
            core.handleDeepLink()
        }
    }

    @JvmOverloads
    fun promoteAnonymousUser(
        options: PromoteOptions,
        onPromoteAnonymousUserListener: OnPromoteAnonymousUserListener,
        handler: Handler = Handler(
            Looper.getMainLooper()
        )
    ) {
        scope.launch {
            try {
                val result = core.promoteAnonymousUser(options)
                handler.post {
                    onPromoteAnonymousUserListener.onPromoted(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onPromoteAnonymousUserListener.onPromotionFailed(e)
                }
            }
        }
    }
}
