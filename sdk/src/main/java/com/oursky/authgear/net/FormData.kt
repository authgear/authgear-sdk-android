package com.oursky.authgear.net

fun Map<String, String>.toFormData(): String {
    return this.toQueryParameter()
}
