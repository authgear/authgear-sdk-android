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
import com.oursky.authgear.data.oauth.OAuthRepoHttp
import kotlinx.coroutines.*
import java.util.*

class Authgear @JvmOverloads
constructor(
    application: Application,
    clientId: String,
    authgearEndpoint: String,
    tokenStorage: TokenStorage = PersistentTokenStorage(application),
    isSsoEnabled: Boolean = false,
    uiVariant: UIVariant = UIVariant.CUSTOM_TABS,
    name: String? = null
) {
    companion object {
        @Suppress("unused")
        private val TAG = Authgear::class.java.simpleName
    }

    internal val core: AuthgearCore

    init {
        this.core = AuthgearCore(
            this,
            application,
            clientId,
            authgearEndpoint,
            isSsoEnabled,
            uiVariant,
            tokenStorage,
            PersistentContainerStorage(application),
            OAuthRepoHttp(),
            KeyRepoKeystore(),
            name
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * OAuth client ID.
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

    val idTokenHint: String?
        @MainThread
        get() {
            return core.idToken
        }

    val canReauthenticate: Boolean
        @MainThread
        get() {
            return core.canReauthenticate
        }

    val authTime: Date?
        @MainThread
        get() {
            return core.authTime
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
     * Authenticate a user by directing the user to an external browser to authenticate.
     * @param options Authenticate options.
     * @param onAuthenticateListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun authenticate(
        options: AuthenticateOptions,
        onAuthenticateListener: OnAuthenticateListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                AuthgearCore.registerWechatRedirectURI(
                    options.wechatRedirectURI,
                    object : AuthgearCore.WechatRedirectHandler {
                        override fun sendWechatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWechatAuthRequest(state)
                            }
                    }
                })
                val userInfo = core.authenticate(options)
                handler.post {
                    onAuthenticateListener.onAuthenticated(userInfo)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    onAuthenticateListener.onAuthenticationFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWechatRedirectURI()
            }
        }
    }

    @MainThread
    @JvmOverloads
    @ExperimentalAuthgearApi
    fun createAuthenticateRequest(
        options: AuthenticateOptions,
        listener: OnCreateAuthenticationRequestListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                val request = core.createAuthenticateRequest(options)
                handler.post {
                    listener.onCreated(request)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    listener.onFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWechatRedirectURI()
            }
        }
    }

    @MainThread
    @JvmOverloads
    @ExperimentalAuthgearApi
    fun finishAuthentication(
        finishUri: String,
        request: AuthenticationRequest,
        listener: OnAuthenticateListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                val userInfo = core.finishAuthorization(finishUri, request.verifier)
                handler.post {
                    listener.onAuthenticated(userInfo)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    listener.onAuthenticationFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWechatRedirectURI()
            }
        }
    }

    /**
     * Reauthenticate the user either by biometric or web.
     */
    @MainThread
    @JvmOverloads
    fun reauthenticate(
        options: ReauthentcateOptions,
        biometricOptions: BiometricOptions?,
        listener: OnReauthenticateListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                AuthgearCore.registerWechatRedirectURI(
                    options.wechatRedirectURI,
                    object : AuthgearCore.WechatRedirectHandler {
                        override fun sendWechatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWechatAuthRequest(state)
                            }
                        }
                    })
                val userInfo = core.reauthenticate(options, biometricOptions)
                handler.post {
                    listener.onFinished(userInfo)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                handler.post {
                    listener.onFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWechatRedirectURI()
            }
        }
    }

    /**
     * Configure authgear. This must be ran before any other methods.
     * If configuration is successful and there was a valid user session, authgear's [accessToken]
     * is non-null and the token is ready to be used.
     *
     * It does local IO to retrieve the refresh token.
     *
     * Therefore, it is possible that configure() could fail for some reasons.
     *
     * configure() can be called more than once if it failed.
     * Otherwise, it is NOT recommended to call it more than once.
     *
     * @param onConfigureListener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    fun configure(
        onConfigureListener: OnConfigureListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.configure()
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
     * [authenticate] or [authenticateAnonymously].
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
                core.refreshAccessTokenIfNeeded()
                handler.post {
                    onRefreshAccessTokenIfNeededListener.onFinished()
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
     * Clear SDK session state. Clear refresh token and reset session state to [SessionState.NO_SESSION].
     */
    @MainThread
    fun clearSessionState() {
        core.clearSessionState()
    }

    /**
     * Generate URL for opening webpage with current session.
     *
     * @param redirectURI URI to be opened in web view
     * @param listener The listener.
     * @param handler The handler of the thread on which the listener is called.
     */
    @MainThread
    @JvmOverloads
    @ExperimentalAuthgearApi
    fun generateUrl(redirectURI: String, listener: OnGenerateURLListener, handler: Handler = Handler(Looper.getMainLooper())) {
        scope.launch {
            try {
                val url = core.generateUrl(redirectURI)
                handler.post {
                    listener?.onGenerated(url)
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
                    listener?.onClosed()
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
                AuthgearCore.registerWechatRedirectURI(
                    options?.wechatRedirectURI,
                    object : AuthgearCore.WechatRedirectHandler {
                        override fun sendWechatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWechatAuthRequest(state)
                            }
                        }
                    }
                )
                core.open(page, options)
                handler.post {
                    listener?.onClosed()
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
                AuthgearCore.registerWechatRedirectURI(
                    options.wechatRedirectURI,
                    object : AuthgearCore.WechatRedirectHandler {
                        override fun sendWechatAuthRequest(state: String) {
                            handler.post {
                                delegate?.sendWechatAuthRequest(state)
                            }
                        }
                    }
                )
                val userInfo = core.promoteAnonymousUser(options)
                handler.post {
                    onPromoteAnonymousUserListener.onPromoted(userInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onPromoteAnonymousUserListener.onPromotionFailed(e)
                }
            } finally {
                AuthgearCore.unregisteredWechatRedirectURI()
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
     * Refresh ID token.
     */
    @MainThread
    @JvmOverloads
    fun refreshIDToken(
        listener: OnRefreshIDTokenListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.refreshIDToken()
                handler.post {
                    listener.onFinished()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    listener.onFailed(e)
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
    fun wechatAuthCallback(
        code: String,
        state: String,
        onWechatAuthCallbackListener: OnWechatAuthCallbackListener,
        handler: Handler = Handler(Looper.getMainLooper())
    ) {
        scope.launch {
            try {
                core.wechatAuthCallback(code, state)
                handler.post {
                    onWechatAuthCallbackListener.onWechatAuthCallback()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    onWechatAuthCallbackListener.onWechatAuthCallbackFailed(e)
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
