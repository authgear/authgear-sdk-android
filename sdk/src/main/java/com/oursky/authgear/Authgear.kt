package com.oursky.authgear

import android.os.Handler
import android.os.Looper
import com.oursky.authgear.data.token.TokenRepoEncryptedSharedPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface OnAuthorizeListener {
    fun onAuthorized(state: String)

    fun onAuthorizationFailed(throwable: Throwable)
}

class Authgear {
    private val core = AuthgearCore(TokenRepoEncryptedSharedPref())
    private val scope = CoroutineScope(Dispatchers.IO)
    val clientId: String?
        get() = core.clientId
    fun authenticateAnonymously() {
        scope.launch {
            core.authenticateAnonymously()
        }
    }
    @JvmOverloads
    fun authorize(onAuthorizeListener: OnAuthorizeListener, handler: Handler = Handler(Looper.getMainLooper())) {
        scope.launch {
            try {
                val result = core.authorize()
                handler.run {
                    onAuthorizeListener.onAuthorized(result)
                }
            } catch (e: Throwable) {
                handler.run {
                    onAuthorizeListener.onAuthorizationFailed(e)
                }
            }
        }
    }
    fun configure(options: ConfigureOptions) {
        scope.launch {
            core.configure(options)
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
