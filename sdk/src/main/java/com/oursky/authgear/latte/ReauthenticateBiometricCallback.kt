package com.oursky.authgear.latte

import androidx.biometric.BiometricPrompt
import com.oursky.authgear.BiometricPromptAuthenticationException
import com.oursky.authgear.isBiometricCancelError
import com.oursky.authgear.wrapException

interface ReauthenticateBiometricCallback {

    @JvmDefault
    fun onAuthenticationFailed(complete: FlowCompletor) {
        // This callback will be invoked EVERY time the recognition failed.
        // So while the prompt is still opened, this callback can be called repetitively.
        // Finally, either onAuthenticationError or onAuthenticationSucceeded will be called.
        // So this callback is not important to the developer.
    }

    @JvmDefault
    fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence,
        complete: FlowCompletor
    ) {
        if (isBiometricCancelError(errorCode)) {
            return
        }
        complete(
            Result.failure(
                wrapException(
                    BiometricPromptAuthenticationException(
                        errorCode
                    )
                )
            )
        )
    }

    @JvmDefault
    fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult, complete: FlowCompletor) {
        complete(Result.success(true))
    }
}

typealias FlowCompletor = (Result<Boolean>) -> Unit
