package com.oursky.authgear

data class SettingOptions @JvmOverloads constructor(
    /**
     * WeChat redirect uri is needed when integrating WeChat login
     * The weChatRedirectURI will be called when user click the login with WeChat button
     */
    var weChatRedirectURI: String? = null
)
