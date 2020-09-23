package com.oursky.authgear.net

internal fun Map<String, String>.toFormData(): String {
    return this.toQueryParameter()
}
