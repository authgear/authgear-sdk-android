package com.oursky.authgear

import android.net.Uri
import android.os.Bundle

@ExperimentalAuthgearApi
data class AuthenticationRequest internal constructor(
    val url: Uri,
    val redirectUri: String,
    internal val verifier: AuthgearCore.Verifier
) {
    companion object {
        private const val KEY_URL = "url"
        private const val KEY_REDIRECT_URI = "redirect_uri"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_CODE_CHALLENGE = "code_challenge"
    }

    constructor(bundle: Bundle) : this(
        Uri.parse(bundle.getString(KEY_URL)!!),
        bundle.getString(KEY_REDIRECT_URI)!!,
        AuthgearCore.Verifier(
            bundle.getString(KEY_CODE_VERIFIER)!!,
            bundle.getString(KEY_CODE_CHALLENGE)!!
        )
    )

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString(KEY_URL, url.toString())
            putString(KEY_REDIRECT_URI, redirectUri)
            putString(KEY_CODE_VERIFIER, verifier.verifier)
            putString(KEY_CODE_CHALLENGE, verifier.challenge)
        }
    }
}
