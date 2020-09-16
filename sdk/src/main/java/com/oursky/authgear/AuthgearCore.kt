package com.oursky.authgear

import com.oursky.authgear.data.token.TokenRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

internal class AuthgearCore(private val tokenRepo: TokenRepo) {
    private var isInitialized = false
    var clientId: String? = null
        private set
    private val scope = CoroutineScope(Dispatchers.IO)
    suspend fun authenticateAnonymously() {
    }
    suspend fun authorize(): String {
        return longWork()
    }
    suspend fun configure(options: ConfigureOptions) {
        if (isInitialized) return
        clientId = options.clientId
    }
    suspend fun logout() {
    }
    suspend fun handleDeepLink() {
    }
    suspend fun promoteAnonymousUser() {
    }
    private suspend fun longWork(): String {
        delay(1000)
        return "Testing"
    }
}
