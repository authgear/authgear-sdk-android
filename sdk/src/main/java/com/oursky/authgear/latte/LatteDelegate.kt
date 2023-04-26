package com.oursky.authgear.latte

interface LatteDelegate {
    @JvmDefault
    fun onTrackingEvent(event: LatteTrackingEvent) {}
}
