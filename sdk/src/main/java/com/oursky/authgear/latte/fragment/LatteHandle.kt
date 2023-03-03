package com.oursky.authgear.latte.fragment

import androidx.fragment.app.Fragment

interface LatteHandle<T> {
    val id: String
    val fragment: Fragment
    suspend fun await(): T
}