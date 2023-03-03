package com.oursky.authgear.latte.fragment

import android.net.Uri
import com.oursky.authgear.*
import com.oursky.authgear.latte.Latte
import java.security.SecureRandom

@OptIn(ExperimentalAuthgearApi::class)
class LattePresenter(val latte: Latte) {
    var delegate: LattePresenterDelegate? = null

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

    suspend fun verifyEmail(email: String): LatteHandle<UserInfo> {
        val entryUrl = "${latte.customUIEndpoint}/verify/email"
        val redirectUri = "${latte.customUIEndpoint}/verify/email/completed"

        val verifyEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()
        val url = latte.authgear.generateUrl(verifyEmailUrl.toString())

        val fragment = LatteUserInfoWebViewFragment(makeID(), url, redirectUri)
        fragment.latte = latte
        return fragment
    }

    suspend fun resetPassword(extraQuery: List<Pair<String, String>>): LatteHandle<Unit> {
        val entryUrl = "${latte.customUIEndpoint}/recovery/reset"
        val redirectUri = "latte://completed"

        val resetPasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            for (q in extraQuery) {
                appendQueryParameter(q.first, q.second)
            }
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()

        val fragment = LatteWebViewFragment(makeID(), resetPasswordUrl, redirectUri)
        fragment.latte = latte
        return fragment
    }

    suspend fun changePassword(): LatteHandle<Unit> {
        val entryUrl = "${latte.customUIEndpoint}/settings/change_password"
        val redirectUri = "latte://completed"

        val changePasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()
        val url = latte.authgear.generateUrl(changePasswordUrl.toString())

        val fragment = LatteWebViewFragment(makeID(), url, redirectUri)
        fragment.latte = latte
        return fragment
    }

    suspend fun changeEmail(email: String, phoneNumber: String): LatteHandle<UserInfo> {
        val entryUrl = "${latte.customUIEndpoint}/settings/change_email"
        val redirectUri = "${latte.customUIEndpoint}/verify/email/completed"

        val changeEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("phone", phoneNumber)
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()
        val url = latte.authgear.generateUrl(changeEmailUrl.toString())

        val fragment = LatteUserInfoWebViewFragment(makeID(), url, redirectUri)
        fragment.latte = latte
        return fragment
    }
}
