package com.oursky.authgear

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import java.security.*

@RequiresApi(api = Build.VERSION_CODES.M)
internal fun makeGenerateKeyPairSpec(alias: String, allowed: Int, invalidatedByBiometricEnrollment: Boolean): KeyGenParameterSpec {
    val builder = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    ) // API Level 23
        .setKeySize(2048) // API Level 23
        .setDigests(KeyProperties.DIGEST_SHA256) // API Level 23
        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) // API Level 23
        .setUserAuthenticationRequired(true) // API Level 23

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setUserAuthenticationParameters(
            0 /* 0 means require authentication every time */,
            allowed
        ) // API Level 30
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Samsung Android 12 treats setUnlockedDeviceRequired in a different way.
        // If setUnlockedDeviceRequired is true, then the device must be unlocked
        // with the same level of security requirement.
        // Otherwise, UserNotAuthenticatedException will be thrown when a cryptographic operation is initialized.
        //
        // The steps to reproduce the bug
        //
        // - Restart the device
        // - Unlock the device with credentials
        // - Create a Signature with a PrivateKey with setUnlockedDeviceRequired(true)
        // - Call Signature.initSign, UserNotAuthenticatedException will be thrown.
        // builder.setUnlockedDeviceRequired(true)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // User confirmation is not needed because the BiometricPrompt itself is a kind of confirmation.
        // builder.setUserConfirmationRequired(true)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // User presence requires a physical button which is not our intended use case.
        // builder.setUserPresenceRequired(true)
    }

    return builder.build()
}

@RequiresApi(api = Build.VERSION_CODES.M)
internal fun createKeyPair(spec: KeyGenParameterSpec): KeyPair {
    val keyPairGenerator: KeyPairGenerator =
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
    keyPairGenerator.initialize(spec)
    return keyPairGenerator.generateKeyPair()
}

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
internal fun removePrivateKey(alias: String) {
    // This function does NOT throw exception if the entry is not found.
    val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    keyStore.deleteEntry(alias)
}

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
internal fun getPrivateKey(alias: String): KeyPair? {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    val entry = keyStore.getEntry(alias, null)
    if (entry !is KeyStore.PrivateKeyEntry) {
        return null
    }
    return KeyPair(entry.certificate.publicKey, entry.privateKey)
}

internal fun makeSignature(privateKey: PrivateKey): Signature {
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initSign(privateKey)
    return signature
}

internal fun authenticatorTypesToKeyProperties(allowed: Int): Int {
    var out = 0
    if ((allowed and BiometricManager.Authenticators.BIOMETRIC_STRONG) != 0) {
        out = out or KeyProperties.AUTH_BIOMETRIC_STRONG
    }
    if ((allowed and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0) {
        out = out or KeyProperties.AUTH_DEVICE_CREDENTIAL
    }
    return out
}

internal fun convertAllowed(allowedAuthenticators: List<BiometricAuthenticator>): Int {
    var flags = 0
    for (allowed in allowedAuthenticators) {
        flags = flags or allowed.raw
    }

    // The official documentation says that on 29 and below,
    // DEVICE_CREDENTIAL and BIOMETRIC_STRONG | DEVICE_CREDENTIAL
    // are unsupported.
    // https://developer.android.com/identity/sign-in/biometric-auth#:~:text=The%20following%20combinations%20of%20authenticator%20types%20aren%27t%20supported%20on%20Android%2010%20(API%20level%2029)%20and%20lower
    if (Build.VERSION.SDK_INT < 30) {
        if (flags == BiometricManager.Authenticators.DEVICE_CREDENTIAL || flags == (BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            // Fallback to BiometricManager.Authenticators.BIOMETRIC_STRONG
            // which is a strong default.
            flags = BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
    }

    return flags
}

internal fun canAuthenticateResultToString(result: Int): String {
    return when (result) {
        BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "BIOMETRIC_STATUS_UNKNOWN"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "BIOMETRIC_ERROR_NO_HARDWARE"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "BIOMETRIC_ERROR_HW_UNAVAILABLE"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "BIOMETRIC_ERROR_NONE_ENROLLED"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "BIOMETRIC_ERROR_UNSUPPORTED"
        else -> ""
    }
}

internal fun authenticateErrorCodeToString(code: Int): String {
    return when (code) {
        BiometricPrompt.ERROR_CANCELED -> "ERROR_CANCELED"
        BiometricPrompt.ERROR_HW_NOT_PRESENT -> "ERROR_HW_NOT_PRESENT"
        BiometricPrompt.ERROR_HW_UNAVAILABLE -> "ERROR_HW_UNAVAILABLE"
        BiometricPrompt.ERROR_LOCKOUT -> "ERROR_LOCKOUT"
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "ERROR_LOCKOUT_PERMANENT"
        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "ERROR_NEGATIVE_BUTTON"
        BiometricPrompt.ERROR_NO_BIOMETRICS -> "ERROR_NO_BIOMETRICS"
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> "ERROR_NO_DEVICE_CREDENTIAL"
        BiometricPrompt.ERROR_NO_SPACE -> "ERROR_NO_SPACE"
        BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> "ERROR_SECURITY_UPDATE_REQUIRED"
        BiometricPrompt.ERROR_TIMEOUT -> "ERROR_TIMEOUT"
        BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "ERROR_UNABLE_TO_PROCESS"
        BiometricPrompt.ERROR_USER_CANCELED -> "ERROR_USER_CANCELED"
        BiometricPrompt.ERROR_VENDOR -> "ERROR_VENDOR"
        else -> ""
    }
}

internal fun buildPromptInfo(
    title: String,
    subtitle: String,
    description: String,
    negativeButtonText: String,
    allowed: Int
): PromptInfo {
    val builder = PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setDescription(description)
        .setAllowedAuthenticators(allowed)
    // DEVICE_CREDENTIAL and negativeButtonText are mutually exclusive.
    // If DEVICE_CREDENTIAL is absent, then negativeButtonText is mandatory.
    // If DEVICE_CREDENTIAL is present, then negativeButtonText must NOT be set.
    if ((allowed and BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
        builder.setNegativeButtonText(negativeButtonText)
    }
    return builder.build()
}

internal fun isBiometricCancelError(errorCode: Int): Boolean {
    if (errorCode == BiometricPrompt.ERROR_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
        return true
    }
    return false
}
