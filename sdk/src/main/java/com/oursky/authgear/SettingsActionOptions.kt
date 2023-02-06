package com.oursky.authgear

internal data class SettingsActionOptions @JvmOverloads constructor(
    var redirectUri: String,
    var colorScheme: ColorScheme? = null,
    var uiLocales: List<String>? = null
)
