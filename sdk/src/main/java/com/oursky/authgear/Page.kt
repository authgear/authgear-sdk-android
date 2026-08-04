package com.oursky.authgear

/**
 * The page to open in external browser.
 */
enum class Page {
    SETTINGS,

    @Deprecated("Use Page.SETTINGS to see a list of identities, and changeEmail / changePhone / etc. to change them.")
    IDENTITY
}
