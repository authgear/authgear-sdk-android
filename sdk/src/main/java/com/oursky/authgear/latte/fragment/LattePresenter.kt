package com.oursky.authgear.latte.fragment

import com.oursky.authgear.*
import com.oursky.authgear.latte.Latte

@OptIn(ExperimentalAuthgearApi::class)
class LattePresenter(val latte: Latte) {
    suspend fun authenticate(options: AuthenticateOptions): LatteFragment<UserInfo> {
        val request = latte.authgear.createAuthenticateRequest(options)
        val fragment = LatteAuthenticateFragment(request)
        fragment.latte = latte
        return fragment
    }
}
