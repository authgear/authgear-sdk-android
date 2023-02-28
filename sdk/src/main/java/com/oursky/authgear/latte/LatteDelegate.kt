package com.oursky.authgear.latte

interface LatteDelegate {
    @JvmDefault
    fun onViewPage(event: LatteViewPageEvent) {}
}
