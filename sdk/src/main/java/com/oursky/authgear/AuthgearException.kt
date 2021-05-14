package com.oursky.authgear

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt

open class AuthgearException : RuntimeException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

internal fun wrapException(e: Exception): Exception {
    // BiometricPrivateKeyNotFoundException
    if (e is KeyPermanentlyInvalidatedException) {
        return BiometricPrivateKeyNotFoundException(e)
    }

    // CancelException
    if (e is BiometricPromptAuthenticationException) {
        if (e.errorCode == BiometricPrompt.ERROR_CANCELED || e.errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || e.errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
            return CancelException(e)
        }
    }

    // BiometricNotSupportedOrPermissionDeniedException
    if (e is BiometricCanAuthenticateException) {
        if (e.result == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE || e.result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE || e.result == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED || e.result == BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED) {
            return BiometricNotSupportedOrPermissionDeniedException(e)
        }
    }
    if (e is BiometricPromptAuthenticationException) {
        if (e.errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT || e.errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE || e.errorCode == BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED) {
            return BiometricNotSupportedOrPermissionDeniedException(e)
        }
    }

    // BiometricNoEnrollmentException
    if (e is BiometricCanAuthenticateException) {
        if (e.result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            return BiometricNoEnrollmentException(e)
        }
    }
    if (e is BiometricPromptAuthenticationException) {
        if (e.errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
            return BiometricNoEnrollmentException(e)
        }
    }

    // BiometricNoPasscodeException
    if (e is BiometricPromptAuthenticationException) {
        if (e.errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
            return BiometricNoPasscodeException(e)
        }
    }

    // BiometricLockoutException
    if (e is BiometricPromptAuthenticationException) {
        if (e.errorCode == BiometricPrompt.ERROR_LOCKOUT || e.errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
            return BiometricLockoutException(e)
        }
    }

    // No wrapping is needed.
    if (e is AuthgearException) {
        return e
    }

    return AuthgearException(e)
}