package com.oursky.authgear.latte

data class ReauthenticateV2Options @JvmOverloads constructor(
    var email: String,
    var phone: String,
    var xState: Map<String, String> = mapOf(),
    var uiLocales: List<String>? = null
)
