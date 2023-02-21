package com.oursky.authgear.latte

import com.oursky.authgear.Authgear

sealed class LatteHandle<T> {
    internal abstract val authgear: Authgear
    internal abstract val broadcastAction: String

    internal data class Success<T>(
        override val authgear: Authgear,
        override val broadcastAction: String,
        val result: T
    ) : LatteHandle<T>()

    internal data class Failure<T>(
        override val authgear: Authgear,
        override val broadcastAction: String,
        val error: Throwable
    ) : LatteHandle<T>()

    val value: T
    get() = when (this) {
        is Success -> this.result
        is Failure -> throw this.error
    }

    fun finish() {
        LatteActivity.dispatch(
            LatteMessage.Finish,
            authgear.core.application,
            broadcastAction
        )
    }
}