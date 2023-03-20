package com.oursky.authgear.latte

import com.oursky.authgear.ColorScheme
import com.oursky.authgear.PromptOption

data class AuthenticateOptions @JvmOverloads constructor(
    var state: String? = null,
    var xState: String? = null,
    var responseType: String? = "code",
    var prompt: List<PromptOption>? = null,
    var loginHint: String? = null,
    var uiLocales: List<String>? = null,
    var colorScheme: ColorScheme? = null,
    var wechatRedirectURI: String? = null,
    var page: String? = null
)

internal fun AuthenticateOptions.toAuthgearAuthenticateOptions(): com.oursky.authgear.AuthenticateOptions {
    return com.oursky.authgear.AuthenticateOptions(
        state = this.state,
        xState = this.xState,
        redirectUri = "latte://complete",
        responseType = this.responseType,
        prompt = this.prompt,
        loginHint = this.loginHint,
        uiLocales = this.uiLocales,
        colorScheme = this.colorScheme,
        wechatRedirectURI = this.wechatRedirectURI,
        page = this.page
    )
}
