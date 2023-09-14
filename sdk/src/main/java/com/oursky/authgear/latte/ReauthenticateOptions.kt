package com.oursky.authgear.latte

data class ReauthenticateOptions @JvmOverloads constructor(
    var email: String,
    var phone: String,
    var xState: Map<String, String> = mapOf(),
    var uiLocales: List<String>? = null,
    var biometricOptions: BiometricOptions? = null,
    var biometricCallback: ReauthenticateBiometricCallback = object : ReauthenticateBiometricCallback {}
)