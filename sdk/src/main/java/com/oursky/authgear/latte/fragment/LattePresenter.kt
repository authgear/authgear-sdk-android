package com.oursky.authgear.latte.fragment

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.oursky.authgear.*
import com.oursky.authgear.latte.Latte
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.security.SecureRandom

@OptIn(ExperimentalAuthgearApi::class)
class LattePresenter(
    val latte: Latte,
    private val appLinkOrigin: Uri,
    private val rewriteAppLinkOrigin: Uri? = null
) {

    var delegate: LattePresenterDelegate? = null
    private val intents = MutableSharedFlow<Intent?>(1, 0, BufferOverflow.DROP_OLDEST)

    private fun makeID(): String {
        val rng = SecureRandom()
        val byteArray = ByteArray(32)
        rng.nextBytes(byteArray)
        val id = base64UrlEncode(byteArray)
        return "latte.$id"
    }

    suspend fun authenticate(options: AuthenticateOptions): LatteHandle<UserInfo> {
        val request = latte.authgear.createAuthenticateRequest(options)
        val fragment = LatteAuthenticateFragment(makeID(), request)
        fragment.latte = latte
        return fragment
    }

    suspend fun verifyEmail(
        email: String,
        state: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<UserInfo> {
        val entryUrl = "${latte.customUIEndpoint}/verify/email"
        val redirectUri = "${latte.customUIEndpoint}/verify/email/completed"

        val verifyEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("redirect_uri", redirectUri)
            if (state != null) {
                appendQueryParameter("state", state)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = latte.authgear.generateUrl(verifyEmailUrl.toString())

        val fragment = LatteUserInfoWebViewFragment(makeID(), url, redirectUri)
        fragment.latte = latte
        return fragment
    }

    suspend fun resetPassword(uri: Uri): LatteHandle<Unit> {
        val entryUrl = "${latte.customUIEndpoint}/recovery/reset"
        val redirectUri = "latte://completed"

        val resetPasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            for (q in uri.getQueryList()) {
                appendQueryParameter(q.first, q.second)
            }
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()

        val fragment = LatteWebViewFragment(makeID(), resetPasswordUrl, redirectUri)
        fragment.latte = latte
        return fragment
    }

    suspend fun changePassword(
        state: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<Unit> {
        val entryUrl = "${latte.customUIEndpoint}/settings/change_password"
        val redirectUri = "latte://completed"

        val changePasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
            if (state != null) {
                appendQueryParameter("state", state)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = latte.authgear.generateUrl(changePasswordUrl.toString())

        val fragment = LatteWebViewFragment(makeID(), url, redirectUri)
        fragment.latte = latte
        return fragment
    }

    suspend fun changeEmail(
        email: String,
        phoneNumber: String,
        state: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<UserInfo> {
        val entryUrl = "${latte.customUIEndpoint}/settings/change_email"
        val redirectUri = "${latte.customUIEndpoint}/verify/email/completed"

        val changeEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("phone", phoneNumber)
            appendQueryParameter("redirect_uri", redirectUri)
            if (state != null) {
                appendQueryParameter("state", state)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = latte.authgear.generateUrl(changeEmailUrl.toString())

        val fragment = LatteUserInfoWebViewFragment(makeID(), url, redirectUri)
        fragment.latte = latte
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
