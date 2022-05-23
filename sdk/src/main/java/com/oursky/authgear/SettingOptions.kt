package com.oursky.authgear

data class SettingOptions @JvmOverloads constructor(
    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The wechatRedirectURI will be called when user click the login with WeChat button
     */
    var wechatRedirectURI: String? = null,
    /**
     * Theme override
     */
    var colorScheme: ColorScheme? = null,
    var uiLocales: List<String>? = null
)
