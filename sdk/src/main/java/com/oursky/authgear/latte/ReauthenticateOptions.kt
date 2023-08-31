package com.oursky.authgear.latte

data class ReauthenticateOptions @JvmOverloads constructor(
    var xSecrets: Map<String, String> = mapOf(),
    var xState: Map<String, String> = mapOf(),
    var uiLocales: List<String>? = null,
    var biometricOptions: BiometricOptions? = null,
    var biometricCallback: ReauthenticateBiometricCallback = object : ReauthenticateBiometricCallback {}
)