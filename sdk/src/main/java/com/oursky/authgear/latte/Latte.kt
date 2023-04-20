package com.oursky.authgear.latte

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.oursky.authgear.*
import com.oursky.authgear.latte.fragment.LatteAuthenticateFragment
import com.oursky.authgear.latte.fragment.LatteUserInfoWebViewFragment
import com.oursky.authgear.latte.fragment.LatteWebViewFragment
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.security.SecureRandom

@OptIn(ExperimentalAuthgearApi::class)
class Latte(
    internal val authgear: Authgear,
    internal val customUIEndpoint: String,
    private val appLinkOrigin: Uri,
    private val rewriteAppLinkOrigin: Uri? = null,
    private val webviewIsInspectable: Boolean = false
) {
    var delegate: LatteDelegate? = null
    private val intents = MutableSharedFlow<Intent?>(1, 0, BufferOverflow.DROP_OLDEST)

    private fun makeID(): String {
        val rng = SecureRandom()
        val byteArray = ByteArray(32)
        rng.nextBytes(byteArray)
        val id = base64UrlEncode(byteArray)
        return "latte.$id"
    }

    suspend fun authenticate(options: AuthenticateOptions): LatteHandle<UserInfo> {
        val request = authgear.createAuthenticateRequest(options.toAuthgearAuthenticateOptions())
        val fragment = LatteAuthenticateFragment(makeID(), request, webviewIsInspectable)
        fragment.latte = this
        return fragment
    }

    suspend fun verifyEmail(
        email: String,
        xState: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<UserInfo> {
        val entryUrl = "$customUIEndpoint/verify/email"
        val redirectUri = "latte://complete"

        val verifyEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("redirect_uri", redirectUri)
            if (xState != null) {
                appendQueryParameter("x_state", xState)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(verifyEmailUrl.toString())

        val fragment = LatteUserInfoWebViewFragment(makeID(), url, redirectUri, webviewIsInspectable)
        fragment.latte = this
        return fragment
    }

    suspend fun resetPassword(uri: Uri): LatteHandle<Unit> {
        val entryUrl = "$customUIEndpoint/recovery/reset"
        val redirectUri = "latte://complete"

        val resetPasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            for (q in uri.getQueryList()) {
                appendQueryParameter(q.first, q.second)
            }
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()

        val fragment = LatteWebViewFragment(makeID(), resetPasswordUrl, redirectUri, webviewIsInspectable)
        fragment.latte = this
        return fragment
    }

    suspend fun changePassword(
        xState: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<Unit> {
        val entryUrl = "$customUIEndpoint/settings/change_password"
        val redirectUri = "latte://complete"

        val changePasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
            if (xState != null) {
                appendQueryParameter("x_state", xState)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(changePasswordUrl.toString())

        val fragment = LatteWebViewFragment(makeID(), url, redirectUri, webviewIsInspectable)
        fragment.latte = this
        return fragment
    }

    suspend fun changeEmail(
        email: String,
        phoneNumber: String,
        xState: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<UserInfo> {
        val entryUrl = "$customUIEndpoint/settings/change_email"
        val redirectUri = "latte://complete"

        val changeEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("phone", phoneNumber)
            appendQueryParameter("redirect_uri", redirectUri)
            if (xState != null) {
                appendQueryParameter("x_state", xState)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(changeEmailUrl.toString())

        val fragment = LatteUserInfoWebViewFragment(makeID(), url, redirectUri, webviewIsInspectable)
        fragment.latte = this
        return fragment
    }

    fun handleIntent(intent: Intent) {
        intents.tryEmit(intent)
    }

    fun listenForAppLinks(
        lifecycleOwner: LifecycleOwner,
        callback: suspend (LatteAppLink) -> Unit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            intents.collect {
                var uri = it?.data ?: return@collect
                val origin = uri.getOrigin() ?: return@collect
                val path = uri.path ?: return@collect
                if (origin != appLinkOrigin.getOrigin()) {
                    return@collect
                }
                if (rewriteAppLinkOrigin != null) {
                    uri = uri.rewriteOrigin(rewriteAppLinkOrigin)
                }

                val link = when {
                    path.endsWith("/reset_link") -> LatteAppLink.ResetLink(uri)
                    path.endsWith("/login_link") -> LatteAppLink.LoginLink(uri)
                    else -> null
                }

                link?.let { callback(link) }
            }
        }
    }
}
