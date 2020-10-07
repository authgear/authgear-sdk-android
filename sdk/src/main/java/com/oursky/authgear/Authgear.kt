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

/**
 * An authgear instance represents a user session. If you need multiple user sessions, simply instantiate multiple authgear instances.
 *
 * Each authgear instance must be identified by a unique name. For simplicity's sake the SDK does not enforce this requirement. Violating
 * this requirement causes undefined behavior.
 *
 * To use authgear, [configure] must be called before any other methods.
 * @param application Application.
 * @param clientId Oauth client ID.
 * @param authgearEndpoint Endpoint of the authgear server.
 * @param name The name of this authgear instance. Must be unique per instance. Default is `default`.
 */
class Authgear @JvmOverloads
constructor(
    application: Application,
    clientId: String,
    authgearEndpoint: String,
    name: String? = null
) {
    companion object {
        @Suppress("unused")
        private val TAG = Authgear::class.java.simpleName
    }

    internal val core =
        AuthgearCore(
            application,
            clientId,
            authgearEndpoint,
            TokenRepoEncryptedSharedPref(application),
            OauthRepoHttp(),
            KeyRepoKeystore(),
            name
        )
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Oauth client ID.
     */
    val clientId: String
        get() {
            return core.clientId
        }

    /**
     * Current session state. See [SessionState].
     */
    val sessionState: SessionState
        get() {
            return core.sessionState
        }

    /**
     * Current access token.
     */
    val accessToken: String?
        get() {
            return core.accessToken
        }

    /**
     * Add a listener to be called when session state is changed.
     *
     * The listener must be removed explicitly with [removeOnSessionStateChangedListener] if
     * the listener is no longer needed to prevent memory leak.
     *
     * For example, if the listener is an anonymous listener in an activity, the listener must be
     * removed in `onDestroy`. If the listener is an anonymous listener in a view model, the
     * listener must be removed in `onCleared`.
     * @param listener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun addOnSessionStateChangedListener(
        listener: OnSessionStateChangedListener,
        handler: Handler = Handler(
                    Looper.getMainLooper()
                )
    ) {
        core.addOnSessionStateChangedListener(listener, handler)
    }

    @MainThread
    fun removeOnSessionStateChangedListener(listener: OnSessionStateChangedListener) {
        core.removeOnSessionStateChangedListener(listener)
    }

    /**
     * Authenticate anonymously by generating a dummy user. The dummy user persist until the app is uninstalled.
     * @param onAuthenticateAnonymouslyListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
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

    /**
     * Authorize a user by directing the user to an external browser to authenticate.
     * @param options Authorize options.
     * @param onAuthorizeListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
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

    /**
     * Configure authgear. This must be ran before any other methods.
     * If configuration is successful and there was a valid user session, authgear's [accessToken]
     * is non-null and the token is ready to be used.
     *
     * When it is not needed to always have the most up-to-date access token during application
     * setup, set [skipRefreshAccessToken] to `true` to bypass refreshing access token. You can
     * later use [refreshAccessTokenIfNeeded] or the synchronous variant
     * [refreshAccessTokenIfNeededSync] to refresh when necessary.
     *
     * It is recommended that the application setup a proper http request abstraction that calls
     * [refreshAccessTokenIfNeededSync] (or its async variant) so that the validity
     * of access token is handled automatically.
     * @param skipRefreshAccessToken `true` to skip refreshing access token during configuration,
     * `false` otherwise.
     * @param onConfigureListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun configure(
        skipRefreshAccessToken: Boolean = false,
        onConfigureListener: OnConfigureListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.configure(skipRefreshAccessToken)
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

    /**
     * Logout the current session by revoking the refresh token and access token. It works for both
     * normal user and anonymous user. For both cases, you can obtain the session again via
     * [authorize] or [authenticateAnonymously].
     *
     * Note that for anonymous user, calling [authenticateAnonymously] after [logout] would login
     * to the same anonymous user.
     *
     * If you need multiple anonymous users, create another authgear instance instead.
     * @param force `true` to ignore any errors and always clear sessions, `false` otherwise.
     * @param onLogoutListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun logout(
        force: Boolean? = null,
        onLogoutListener: OnLogoutListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.logout(force)
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
     * Refresh access token when necessary synchronously if needed. Do *NOT* call this on the main
     * thread.
     */
    @WorkerThread
    fun refreshAccessTokenIfNeededSync(): String? {
        return runBlocking {
            withContext(scope.coroutineContext) {
                core.refreshAccessTokenIfNeeded()
            }
        }
    }

    /**
     * Refresh access token when necessary.
     * @param onRefreshAccessTokenIfNeededListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
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

    /**
     * Open the specific path on the authgear server.
     */
    fun openUrl(path: String) {
        core.openUrl(path)
    }

    /**
     * Open the specific [Page] in external browser.
     */
    fun open(page: Page) {
        core.open(page)
    }

    /**
     * Promote the current anonymous user. Note that this must not be called before there is an
     * anonymous user.
     *
     * This can be called either when the anonymous user is logged in or logged out.
     *
     * If the promotions succeeds, the anonymous user would be reset and a new anonymous can be
     * created by [authenticateAnonymously].
     * @param options Promotion options.
     * @param onPromoteAnonymousUserListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
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

    /**
     * Fetch user info. Note that this method does not refresh access token so user should call
     * [configure] or [refreshAccessTokenIfNeeded] when necessary.
     * @param onFetchUserInfoListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
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
