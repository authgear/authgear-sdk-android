@file:JvmName("KotlinExtensions")

package com.oursky.authgear

/**
 * @see [Authgear.configure].
 */
suspend fun Authgear.configure() {
    return core.configure()
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
    return core.fetchUserInfo()
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
