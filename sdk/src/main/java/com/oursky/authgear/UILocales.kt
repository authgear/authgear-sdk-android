package com.oursky.authgear

object UILocales {
    fun stringify(uiLocales: List<String>?): String {
        return uiLocales?.joinToString(separator = " ") ?: ""
    }
}
