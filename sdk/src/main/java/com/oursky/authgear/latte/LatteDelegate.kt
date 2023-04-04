package com.oursky.authgear.latte

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

interface LatteDelegate {
    val latteFragmentManager: FragmentManager
    fun showLatteFragment(id: String, fragment: Fragment)
    fun hideLatteFragment(id: String)

    @JvmDefault
    fun onTrackingEvent(event: LatteTrackingEvent) {}
}
