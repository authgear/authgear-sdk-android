package com.oursky.authgear.latte

import com.oursky.authgear.ColorScheme
import com.oursky.authgear.PromptOption

data class AuthenticateOptions @JvmOverloads constructor(
    var xSecrets: Map<String, String> = mapOf(),
    var xState: Map<String, String> = mapOf(),
    var prompt: List<PromptOption>? = null,
    var uiLocales: List<String>? = null,
    var colorScheme: ColorScheme? = null,
    var wechatRedirectURI: String? = null,
    var page: String? = null
)
