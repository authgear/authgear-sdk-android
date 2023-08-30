@file:JvmName("KotlinExtensions")

package com.oursky.authgear

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.oursky.authgear.app2app.App2AppAuthenticateOptions
import com.oursky.authgear.app2app.App2AppAuthenticateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @see [Authgear.configure].
 */
suspend fun Authgear.configure() {
    return withContext(Dispatchers.IO) {
        core.configure()
    }
}

/**
 * @see [Authgear.refreshAccessTokenIfNeededSync].
 */
suspend fun Authgear.refreshTokenIfNeeded(): String? {
    return core.refreshAccessTokenIfNeeded()
}

/**
 * @see [Authgear.authenticate].
 */
suspend fun Authgear.authenticate(options: AuthenticateOptions): UserInfo {
    return core.authenticate(options)
}

/**
 * @see [Authgear.createAuthenticateRequest].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.createAuthenticateRequest(options: AuthenticateOptions): AuthenticationRequest {
    return withContext(Dispatchers.IO) {
        core.createAuthenticateRequest(options)
    }
}

/**
 * @see [Authgear.createReauthenticateRequest].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.createReauthenticateRequest(options: ReauthentcateOptions): AuthenticationRequest {
    return withContext(Dispatchers.IO) {
        core.createReauthenticateRequest(options)
    }
}

/**
 * @see [Authgear.finishAuthentication].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.finishAuthentication(
    finishUri: String,
    request: AuthenticationRequest
): UserInfo {
    return withContext(Dispatchers.IO) {
        core.finishAuthorization(finishUri, request.verifier)
    }
}

/**
 * @see [Authgear.authenticateAnonymously]
 */
suspend fun Authgear.authenticateAnonymousLy(): UserInfo {
    return core.authenticateAnonymously()
}

/**
 * @see [Authgear.logout]
 */
suspend fun Authgear.logout(force: Boolean? = null) {
    return core.logout(force)
}

/**
 * @see [Authgear.promoteAnonymousUser].
 */
suspend fun Authgear.promoteAnonymousUser(options: PromoteOptions): UserInfo {
    return core.promoteAnonymousUser(options)
}

/**
 * @see [Authgear.fetchUserInfo].
 */
suspend fun Authgear.fetchUserInfo(): UserInfo {
    return withContext(Dispatchers.IO) {
        core.fetchUserInfo()
    }
}

/**
 * @see [Authgear.enableBiometric]
 */
suspend fun Authgear.enableBiometric(options: BiometricOptions) {
    core.enableBiometric(options)
}

/**
 * @see [Authgear.authenticateBiometric]
 */
suspend fun Authgear.authenticateBiometric(options: BiometricOptions): UserInfo {
    return core.authenticateBiometric(options)
}

/**
 * @see [Authgear.startApp2AppAuthentication]
 */
@RequiresApi(Build.VERSION_CODES.M)
suspend fun Authgear.startApp2AppAuthentication(options: App2AppAuthenticateOptions): UserInfo {
    return core.startApp2AppAuthentication(options)
}

/**
 * @see [Authgear.approveApp2AppAuthenticationRequest]
 */
@RequiresApi(Build.VERSION_CODES.M)
suspend fun Authgear.approveApp2AppAuthenticationRequest(request: App2AppAuthenticateRequest) {
    return core.approveApp2AppAuthenticationRequest(request)
}

/**
 * @see [Authgear.rejectApp2AppAuthenticationRequest]
 */
@RequiresApi(Build.VERSION_CODES.M)
suspend fun Authgear.rejectApp2AppAuthenticationRequest(request: App2AppAuthenticateRequest, reason: Throwable) {
    return core.rejectApp2AppAuthenticationRequest(request, reason)
}

/**
 * @see [Authgear.generateUrl].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.generateUrl(redirectURI: String): Uri {
    return withContext(Dispatchers.IO) {
        core.generateUrl(redirectURI)
    }
}