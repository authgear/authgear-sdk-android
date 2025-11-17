package com.oursky.authgear

import androidx.fragment.app.FragmentActivity

data class BiometricOptions constructor(
    var activity: FragmentActivity,
    var title: String,
    var subtitle: String,
    var description: String,
    var negativeButtonText: String,
    var allowedAuthenticatorsOnEnable: List<BiometricAuthenticator>,
    var allowedAuthenticatorsOnAuthenticate: List<BiometricAuthenticator>,
    var invalidatedByBiometricEnrollment: Boolean
)