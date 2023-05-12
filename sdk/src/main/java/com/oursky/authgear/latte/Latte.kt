package com.oursky.authgear.latte

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.oursky.authgear.*
import com.oursky.authgear.data.HttpClient
import com.oursky.authgear.latte.fragment.LatteFragment
import com.oursky.authgear.net.toQueryParameter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalAuthgearApi::class)
class Latte(
    internal val authgear: Authgear,
    internal val customUIEndpoint: String,
    internal val tokenizeEndpoint: String,
    private val appLinkOrigin: Uri,
    private val rewriteAppLinkOrigin: Uri? = null,
    private val webContentsDebuggingEnabled: Boolean = false
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

    private suspend fun waitWebViewToLoad(webView: WebView) {
        suspendCoroutine<Unit> { k ->
            var isResumed = false
            webView.onReady = fun(_) {
                if (isResumed) {
                    return
                }
                isResumed = true
                k.resume(Unit)
            }
            webView.completion = fun(_, result) {
                if (isResumed) {
                    return
                }
                isResumed = true
                k.resumeWith(result.mapCatching {
                    // Throw error if needed
                    it.getOrThrow()
                    return
                })
            }
            webView.load()
        }
    }

    suspend fun preload(context: Context) {
        val url = Uri.parse(this.customUIEndpoint + "/preload")
        val redirectUri = "latte://complete"
        val webView = WebView(context, WebViewRequest(url = url, redirectUri = redirectUri), webContentsDebuggingEnabled)
        this.waitWebViewToLoad(webView)
    }

    suspend fun authenticate(context: Context, lifecycleOwner: LifecycleOwner, options: AuthenticateOptions): Pair<Fragment, LatteHandle<UserInfo>> {
        val request = authgear.createAuthenticateRequest(makeAuthgearAuthenticateOptions(options))
        val fragment = LatteFragment(
            context = context,
            id = makeID(),
            url = request.url,
            redirectUri = request.redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        fragment.latte = this

        this.waitWebViewToLoad(fragment.webView)

        val d = lifecycleOwner.lifecycleScope.async {
            val result: Uri = suspendCoroutine<WebViewResult> { k ->
                fragment.webView.completion = { _, result ->
                    k.resumeWith(result)
                }
            }.getOrThrow()
            val userInfo = authgear.finishAuthentication(result.toString(), request)
            userInfo
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun verifyEmail(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        email: String,
        xState: Map<String, String> = mapOf(),
        uiLocales: List<String>? = null
    ): Pair<Fragment, LatteHandle<UserInfo>> {
        val entryUrl = "$customUIEndpoint/verify/email"
        val redirectUri = "latte://complete"
        val xSecrets = hashMapOf(
            "email" to email
        )
        val finalXState = makeXStateWithSecrets(xState, xSecrets)

        val verifyEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
            if (xState.isNotEmpty()) {
                appendQueryParameter("x_state", finalXState.toQueryParameter())
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(verifyEmailUrl.toString())

        val fragment = LatteFragment(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        fragment.latte = this

        this.waitWebViewToLoad(fragment.webView)

        val d = lifecycleOwner.lifecycleScope.async {
            suspendCoroutine<WebViewResult> { k ->
                fragment.webView.completion = { _, result ->
                    k.resumeWith(result)
                }
            }.getOrThrow()
            val userInfo = authgear.fetchUserInfo()
            userInfo
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun resetPassword(context: Context, lifecycleOwner: LifecycleOwner, uri: Uri): Pair<Fragment, LatteHandle<Unit>> {
        val entryUrl = "$customUIEndpoint/recovery/reset"
        val redirectUri = "latte://complete"

        val resetPasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            for (q in uri.getQueryList()) {
                appendQueryParameter(q.first, q.second)
            }
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()

        val fragment = LatteFragment(
            context = context,
            id = makeID(),
            url = resetPasswordUrl,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        fragment.latte = this

        this.waitWebViewToLoad(fragment.webView)

        val d = lifecycleOwner.lifecycleScope.async {
            suspendCoroutine<WebViewResult> { k ->
                fragment.webView.completion = { _, result ->
                    k.resumeWith(result)
                }
            }.getOrThrow()
            Unit
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun changePassword(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        xState: Map<String, String> = mapOf(),
        uiLocales: List<String>? = null
    ): Pair<Fragment, LatteHandle<Unit>> {
        val entryUrl = "$customUIEndpoint/settings/change_password"
        val redirectUri = "latte://complete"

        val changePasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
            if (xState.isNotEmpty()) {
                appendQueryParameter("x_state", xState.toQueryParameter())
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(changePasswordUrl.toString())

        val fragment = LatteFragment(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        fragment.latte = this

        this.waitWebViewToLoad(fragment.webView)

        val d = lifecycleOwner.lifecycleScope.async {
            suspendCoroutine<WebViewResult> { k ->
                fragment.webView.completion = { _, result ->
                    k.resumeWith(result)
                }
            }.getOrThrow()
            Unit
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun changeEmail(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        email: String,
        phoneNumber: String,
        xState: Map<String, String> = mapOf(),
        uiLocales: List<String>? = null
    ): Pair<Fragment, LatteHandle<UserInfo>> {
        val entryUrl = "$customUIEndpoint/settings/change_email"
        val redirectUri = "latte://complete"

        val xSecrets = hashMapOf(
            "email" to email,
            "phone" to phoneNumber
        )
        val finalXState = makeXStateWithSecrets(xState, xSecrets)

        val changeEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
            if (xState.isNotEmpty()) {
                appendQueryParameter("x_state", finalXState.toQueryParameter())
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(changeEmailUrl.toString())

        val fragment = LatteFragment(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        fragment.latte = this

        this.waitWebViewToLoad(fragment.webView)

        val d = lifecycleOwner.lifecycleScope.async {
            suspendCoroutine<WebViewResult> { k ->
                fragment.webView.completion = { _, result ->
                    k.resumeWith(result)
                }
            }.getOrThrow()
            val userInfo = authgear.fetchUserInfo()
            userInfo
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
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

    private suspend fun createXSecretsToken(data: ByteArray): String {
        return withContext(Dispatchers.IO) {
            val url = URL(this@Latte.tokenizeEndpoint)
            return@withContext HttpClient.fetch(url, "POST", emptyMap()) { conn ->
                conn.outputStream.use {
                    it.write(data)
                }
                conn.errorStream?.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                }
                conn.inputStream.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                    return@fetch responseString
                }
            }
        }
    }

    private suspend fun makeXStateWithSecrets(
        xState: Map<String, String>,
        xSecrets: Map<String, String>
    ): Map<String, String> {
        val finalXState = HashMap(xState)
        if (xSecrets.isNotEmpty()) {
            val xTokenJson = JSONObject(xSecrets).toString()
            val xSecretsToken = createXSecretsToken(xTokenJson.toByteArray(StandardCharsets.UTF_8))
            finalXState["x_secrets_token"] = xSecretsToken
        }
        return finalXState
    }

    private suspend fun makeAuthgearAuthenticateOptions(latteOptions: AuthenticateOptions): com.oursky.authgear.AuthenticateOptions {
        val finalXState = makeXStateWithSecrets(latteOptions.xState, latteOptions.xSecrets)

        return AuthenticateOptions(
            xState = finalXState.toQueryParameter(),
            redirectUri = "latte://complete",
            responseType = latteOptions.responseType,
            prompt = latteOptions.prompt,
            loginHint = latteOptions.loginHint,
            uiLocales = latteOptions.uiLocales,
            colorScheme = latteOptions.colorScheme,
            wechatRedirectURI = latteOptions.wechatRedirectURI,
            page = latteOptions.page
        )
    }
}
