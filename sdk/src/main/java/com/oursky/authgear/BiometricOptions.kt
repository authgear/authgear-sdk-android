package com.oursky.authgear

import androidx.fragment.app.FragmentActivity

data class BiometricOptions @JvmOverloads constructor(
    var activity: FragmentActivity,
    var title: String,
    var subtitle: String,
    var description: String,
    var negativeButtonText: String,
    var allowedAuthenticatorsOnEnable: List<BiometricAuthenticator>,
    var allowedAuthenticatorsOnAuthenticate: List<BiometricAuthenticator>,
    var invalidatedByBiometricEnrollment: Boolean,
    /**
     * Resource indicator (RFC 8707) used by [Authgear.authenticateBiometric]. Defaults to the `resource` given to `configure`; a value given here replaces it.
     * It is never sent when enabling biometric.
     */
    var resource: String? = null,
    /**
     * Scopes of the API Resource given in [resource] to request in [Authgear.authenticateBiometric]. Defaults to the `scopes` given to `configure`; a value given here replaces it.
     * They are merged with the scopes required by the SDK.
     */
    var scopes: List<String>? = null
)