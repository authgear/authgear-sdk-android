package com.oursky.authgear.latte

import androidx.fragment.app.FragmentActivity

data class BiometricOptions constructor(
    var activity: FragmentActivity,
    var title: String,
    var subtitle: String? = null,
    var description: String? = null,
    var negativeButtonText: String? = null,
    var allowedAuthenticators: Int = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
)
