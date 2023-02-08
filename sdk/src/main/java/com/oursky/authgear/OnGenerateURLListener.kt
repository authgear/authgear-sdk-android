package com.oursky.authgear

@ExperimentalAuthgearApi
interface OnGenerateURLListener {
    fun onGenerated(url: String)
    fun onFailed(throwable: Throwable)
}