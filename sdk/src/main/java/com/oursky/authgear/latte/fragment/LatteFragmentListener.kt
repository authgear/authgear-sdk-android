package com.oursky.authgear.latte.fragment

interface LatteFragmentListener<T> {
    fun onReauthWithBiometric(resumeWith: (Result<T>) -> Unit) {}
}