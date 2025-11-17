package com.oursky.authgear

import androidx.biometric.BiometricManager

enum class BiometricAuthenticator(val raw: Int) {
    BIOMETRIC_STRONG(BiometricManager.Authenticators.BIOMETRIC_STRONG),
    DEVICE_CREDENTIAL(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
}