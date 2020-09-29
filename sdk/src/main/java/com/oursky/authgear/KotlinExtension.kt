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
 * @see [Authgear.authorize].
 */
suspend fun Authgear.authorize(options: AuthorizeOptions): String? {
    return core.authorize(options)
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
suspend fun Authgear.promoteAnonymousUser(options: PromoteOptions): AuthorizeResult {
    return core.promoteAnonymousUser(options)
}

/**
 * @see [Authgear.fetchUserInfo].
 */
suspend fun Authgear.fetchUserInfo(): UserInfo {
    return core.fetchUserInfo()
}