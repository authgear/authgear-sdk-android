package com.oursky.authgear.latte

import kotlinx.serialization.Serializable

@Serializable
internal sealed class LatteMessage {
    @Serializable
    object OpenEmailClient : LatteMessage()

    @Serializable
    data class ViewPage(val event: LatteViewPageEvent) : LatteMessage()

    @Serializable
    data class HandleRedirectURI(val finishUri: String?) : LatteMessage()

    @Serializable
    object Finish : LatteMessage()
}
