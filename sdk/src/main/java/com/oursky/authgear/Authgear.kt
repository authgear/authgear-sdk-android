package com.oursky.authgear

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
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
            this,
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
        @MainThread
        get() {
            return core.clientId
        }

    /**
     * Current session state. See [SessionState].
     */
    val sessionState: SessionState
        @MainThread
        get() {
            return core.sessionState
        }

    /**
     * Current access token.
     */
    val accessToken: String?
        @MainThread
        get() {
            return core.accessToken
        }

    var delegate: AuthgearDelegate?
        @MainThread
        get() {
            return core.delegate
        }
        @MainThread
        set(value) {
            core.delegate = value
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
                AuthgearCore.registerWeChatRedirectURI(
                    options.weChatRedirectURI,
                    object : AuthgearCore.WeChatRedirectHandler {
                        override fun sendWeChatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWeChatAuthRequest(state)
                            }
                    }
                })
                val result = core.authorize(options)
                handler.post {
                    onAuthorizeListener.onAuthorized(result)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    onAuthorizeListener.onAuthorizationFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWeChatRedirectURI()
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
     *
     * It does local IO to retrieve the refresh token.
     * It also does network IO to refresh the access token.
     *
     * Therefore, it is possible that configure() could fail for many reasons.
     * If your application is offline first, be prepared for handling errors.
     *
     * configure() can be called more than once if it failed.
     * Otherwise, it is NOT recommended to call it more than once.
     *
     * @param skipRefreshAccessToken `true` to skip refreshing access token during configuration,
     * `false` otherwise.
     * @param onConfigureListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun configure(
        skipRefreshAccessToken: Boolean = false,
        transientSession: Boolean = false,
        onConfigureListener: OnConfigureListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.configure(skipRefreshAccessToken, transientSession)
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
     *
     * @param path Path to be opened in web view
     * @param listener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun openUrl(path: String, listener: OnOpenURLListener? = null, handler: Handler = Handler(Looper.getMainLooper())) {
        scope.launch {
            try {
                core.openUrl(path)
                handler.post {
                    listener?.onOpened()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    listener?.onFailed(e)
                }
            }
        }
    }

    /**
     * Open the specific [Page] in web view.
     *
     * @param page Page in Authgear Web UI.
     * @param options Setting options.
     * @param listener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun open(
        page: Page,
        options: SettingOptions? = null,
        listener: OnOpenURLListener? = null,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                AuthgearCore.registerWeChatRedirectURI(
                    options?.weChatRedirectURI,
                    object : AuthgearCore.WeChatRedirectHandler {
                        override fun sendWeChatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWeChatAuthRequest(state)
                            }
                        }
                    }
                )
                core.open(page, options)
                handler.post {
                    listener?.onOpened()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    listener?.onFailed(e)
                }
            }
        }
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
                AuthgearCore.registerWeChatRedirectURI(
                    options.weChatRedirectURI,
                    object : AuthgearCore.WeChatRedirectHandler {
                        override fun sendWeChatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWeChatAuthRequest(state)
                            }
                        }
                    }
                )
                val result = core.promoteAnonymousUser(options)
                handler.post {
                    onPromoteAnonymousUserListener.onPromoted(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onPromoteAnonymousUserListener.onPromotionFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWeChatRedirectURI()
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

    /**
     * WeChat auth callback function. In WeChat login flow, after returning from the WeChat SDK,
     * this function should be called to complete the authorization.
     * @param code WeChat Authorization code.
     * @param state WeChat Authorization state.
     */
    @MainThread
    @JvmOverloads
    fun weChatAuthCallback(
        code: String,
        state: String,
        onWeChatAuthCallbackListener: OnWeChatAuthCallbackListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.weChatAuthCallback(code, state)
                handler.post {
                    onWeChatAuthCallbackListener.onWeChatAuthCallback()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onWeChatAuthCallbackListener.onWeChatAuthCallbackFailed(e)
                }
            }
        }
    }

    /**
     * Check if biometric is supported. If not supported, an exception will be thrown.
     */
    @MainThread
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun checkBiometricSupported(context: Context, allowedAuthenticators: Int) {
        core.checkBiometricSupported(context, allowedAuthenticators)
    }

    /**
     * Check if biometric is enabled.
     */
    @MainThread
    fun isBiometricEnabled(): Boolean {
        return core.isBiometricEnabled()
    }

    /**
     * Disable biometric if it was enabled.
     */
    @MainThread
    fun disableBiometric() {
        core.disableBiometric()
    }

    /**
     * Enable biometric for the current user.
     */
    @MainThread
    @JvmOverloads
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun enableBiometric(
        options: BiometricOptions,
        onEnableBiometricListener: OnEnableBiometricListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.enableBiometric(options)
                handler.post {
                    onEnableBiometricListener.onEnabled()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onEnableBiometricListener.onFailed(e)
                }
            }
        }
    }

    /**
     * Authenticate with previously enabled biometric.
     */
    @MainThread
    @JvmOverloads
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun authenticateBiometric(
        options: BiometricOptions,
        onAuthenticateBiometricListener: OnAuthenticateBiometricListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                val userInfo = core.authenticateBiometric(options)
                handler.post {
                    onAuthenticateBiometricListener.onAuthenticated(userInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onAuthenticateBiometricListener.onAuthenticationFailed(e)
                }
            }
        }
    }
}
