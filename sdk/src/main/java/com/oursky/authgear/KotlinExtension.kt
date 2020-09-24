@file:JvmName("KotlinExtensions")

package com.oursky.authgear

suspend fun Authgear.configure(options: ConfigureOptions) {
    return core.configure(options)
}

suspend fun Authgear.refreshTokenIfNeeded(): String? {
    return core.refreshAccessTokenIfNeeded()
}

suspend fun Authgear.authorize(options: AuthorizeOptions): String? {
    return core.authorize(options)
}

suspend fun Authgear.authenticateAnonymousLy(): UserInfo {
    return core.authenticateAnonymously()
}

suspend fun Authgear.logout(force: Boolean? = null) {
    return core.logout(force)
}

suspend fun Authgear.promoteAnonymousUser(options: PromoteOptions): AuthorizeResult {
    return core.promoteAnonymousUser(options)
}

suspend fun Authgear.fetchUserInfo(): UserInfo {
    return core.fetchUserInfo()
}