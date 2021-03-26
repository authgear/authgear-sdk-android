package com.oursky.authgear

import androidx.fragment.app.FragmentActivity

data class BiometricOptions constructor(
    var activity: FragmentActivity,
    var title: String,
    var subtitle: String,
    var description: String,
    var negativeButtonText: String,
    var allowedAuthenticators: Int,
    var invalidatedByBiometricEnrollment: Boolean
)