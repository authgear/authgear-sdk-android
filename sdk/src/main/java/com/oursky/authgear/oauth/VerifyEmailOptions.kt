package com.oursky.authgear.oauth

import com.oursky.authgear.ColorScheme
import com.oursky.authgear.SettingsActionOptions

data class VerifyEmailOptions @JvmOverloads constructor(
    /**
     * Redirection URI to which the response will be sent after performing action.
     */
    var redirectUri: String,
    /**
     * Theme override
     */
    var colorScheme: ColorScheme? = null,
    var uiLocales: List<String>? = null
)

internal fun VerifyEmailOptions.toSettingsActionOptions(): SettingsActionOptions {
    return SettingsActionOptions(
        redirectUri = this.redirectUri,
        colorScheme = this.colorScheme,
        uiLocales = this.uiLocales
    )
}
