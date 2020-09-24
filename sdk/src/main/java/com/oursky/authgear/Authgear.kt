package com.oursky.authgear

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.oursky.authgear.data.key.KeyRepoKeystore
import com.oursky.authgear.data.oauth.OauthRepoHttp
import com.oursky.authgear.data.token.TokenRepoEncryptedSharedPref
import kotlinx.coroutines.*

class Authgear @JvmOverloads constructor(application: Application, name: String? = null) {
    companion object {
        @Suppress("unused")
        private val TAG = Authgear::class.java.simpleName
    }

    internal val core =
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
    val accessToken: String?
        get() {
            return core.accessToken
        }

    @MainThread
    @JvmOverloads
    fun setOnRefreshTokenExpiredListener(
        listener: OnRefreshTokenExpiredListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        core.onRefreshTokenExpiredListener = ListenerPair(listener, handler)
    }

    @MainThread
    @JvmOverloads
    fun authenticateAnonymously(
        onAuthenticateAnonymouslyListener: OnAuthenticateAnonymouslyListener,
        handler: Handler = Handler(Looper.getMainLooper())
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

    @MainThread
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

    @MainThread
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

    @MainThread
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

    /**
     * Refresh access token synchronously if needed. Do *NOT* call this on the main thread.
     */
    @WorkerThread
    fun refreshAccessTokenIfNeededSync(): String? {
        return runBlocking {
            withContext(scope.coroutineContext) {
                core.refreshAccessTokenIfNeeded()
            }
        }
    }

    @MainThread
    @JvmOverloads
    fun refreshAccessTokenIfNeeded(
        onRefreshAccessTokenIfNeededListener: OnRefreshAccessTokenIfNeededListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                val token = core.refreshAccessTokenIfNeeded()
                handler.post {
                    onRefreshAccessTokenIfNeededListener.onFinished(token)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    onRefreshAccessTokenIfNeededListener.onFailed(e)
                }
            }
        }
    }

    fun handleDeepLink() {
        scope.launch {
            core.handleDeepLink()
        }
    }

    @MainThread
    @JvmOverloads
    fun promoteAnonymousUser(
        options: PromoteOptions,
        onPromoteAnonymousUserListener: OnPromoteAnonymousUserListener,
        handler: Handler = Handler(Looper.getMainLooper())
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

    @MainThread
    @JvmOverloads
    fun fetchUserInfo(
        onFetchUserInfoListener: OnFetchUserInfoListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                val userInfo = core.fetchUserInfo()
                handler.post {
                    onFetchUserInfoListener.onFetchedUserInfo(userInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onFetchUserInfoListener.onFetchingUserInfoFailed(e)
                }
            }
        }
    }
}
