package com.oursky.authgear.latte

import androidx.fragment.app.Fragment

interface LatteDelegate {
    fun showLatteFragment(id: String, fragment: Fragment)
    fun hideLatteFragment(id: String)

    @JvmDefault
    fun onAnalyticsEvent(event: LatteAnalyticsEvent) {}
}
