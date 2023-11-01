package com.oursky.authgear.latte

import kotlinx.coroutines.Deferred

data class LatteHandle<T>(val fragmentId: String, private val deferred: Deferred<T>) {
    suspend fun await(): T {
        return deferred.await()
    }
}
