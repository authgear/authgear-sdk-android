package com.oursky.authgear.latte.fragment

import androidx.fragment.app.Fragment
import com.oursky.authgear.*

interface LattePresenterDelegate {
    fun showLatteFragment(id: String, fragment: Fragment)
    fun hideLatteFragment(id: String)
}
