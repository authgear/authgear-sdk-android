package com.oursky.authgear.latte

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.oursky.authgear.*
import com.oursky.authgear.data.HttpClient
import com.oursky.authgear.latte.fragment.LatteFragment
import com.oursky.authgear.latte.fragment.LatteFragmentListener
import com.oursky.authgear.net.toQueryParameter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

@OptIn(ExperimentalAuthgearApi::class)
class Latte(
    internal val authgear: Authgear,
    internal val customUIEndpoint: String,
    internal val tokenizeEndpoint: String,
    private val appLinkOrigin: Uri,
    private val shortLinkOrigin: Uri,
    private val rewriteAppLinkOrigin: Uri? = null,
    private val rewriteShortLinkOrigin: Uri? = null,
    private val webContentsDebuggingEnabled: Boolean = false,
    private val webViewLoadTimeoutMillis: Long = 15000
) {
    var delegate: LatteDelegate? = null
    private val intents = MutableSharedFlow<Intent?>(1, 0, BufferOverflow.DROP_OLDEST)

    internal enum class BroadcastType(val action: String) {
        RESET_PASSWORD_COMPLETED("com.oursky.authgear.latte.RESET_PASSWORD_COMPLETED")
    }

    private fun makeID(): String {
        val rng = SecureRandom()
        val byteArray = ByteArray(32)
        rng.nextBytes(byteArray)
        val id = base64UrlEncode(byteArray)
        return "latte.$id"
    }

    suspend fun preload(context: Context) {
        val url = Uri.parse(this.customUIEndpoint + "/preload")
        val redirectUri = "latte://complete"
        LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        ).waitWebViewToLoad(webViewLoadTimeoutMillis)
    }

    private suspend fun waitForResult(handle: LatteFragment.ListenHandle): LatteResult {
        return handle.waitForResult(null) { result, resumeWith ->
            resumeWith(Result.success(result))
        }
    }

    suspend fun authenticate(context: Context, coroutineScope: CoroutineScope, options: AuthenticateOptions): Pair<Fragment, LatteHandle<UserInfo>> {
        val request = authgear.createAuthenticateRequest(makeAuthgearAuthenticateOptions(options))
        val fragment = LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = request.url,
            redirectUri = request.redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        val listenHandle = fragment.listen(authgear.core.application, this)
        fragment.waitWebViewToLoad(webViewLoadTimeoutMillis)

        val d = coroutineScope.async {
            val result = waitForResult(listenHandle)
            val userInfo = authgear.finishAuthentication(result.getOrThrow().toString(), request)
            userInfo
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun reauthenticate(
        context: Context,
        coroutineScope: CoroutineScope,
        options: ReauthenticateOptions
    ): Pair<Fragment, LatteHandle<Boolean>> {
        val request = authgear.createReauthenticateRequest(makeAuthgearReauthenticateOptions(context, options))
        val fragment = LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = request.url,
            redirectUri = request.redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        val listenHandle = fragment.listen(authgear.core.application, this)
        fragment.waitWebViewToLoad(webViewLoadTimeoutMillis)

        val listener = object : LatteFragmentListener<Boolean> {
            override fun onReauthWithBiometric(resumeWith: (Result<Boolean>) -> Unit) {
                val biometricOptions = options.biometricOptions ?: return
                val builder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(biometricOptions.title)
                    .setAllowedAuthenticators(biometricOptions.allowedAuthenticators)
                val subtitle = biometricOptions.subtitle
                if (subtitle != null) {
                    builder.setSubtitle(subtitle)
                }
                val description = biometricOptions.description
                if (description != null) {
                    builder.setSubtitle(description)
                }
                val negativeButtonText = biometricOptions.negativeButtonText
                if (negativeButtonText != null) {
                    builder.setNegativeButtonText(negativeButtonText)
                }
                val promptInfo = builder.build()
                val callback = options.biometricCallback
                val prompt =
                    BiometricPrompt(
                        biometricOptions.activity,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationFailed() {
                                callback.onAuthenticationFailed(resumeWith)
                            }

                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence
                            ) {
                                callback.onAuthenticationError(errorCode, errString, resumeWith)
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                callback.onAuthenticationSucceeded(result, resumeWith)
                            }
                        })

                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    prompt.authenticate(promptInfo)
                }
            }
        }

        val d = coroutineScope.async {
            val result = listenHandle.waitForResult<Boolean>(listener) { r, resumeWith ->
                val uri = r.getOrThrow()
                coroutineScope.launch {
                    try {
                        authgear.finishReauthentication(uri.toString(), request)
                        resumeWith(Result.success(true))
                    } catch (e: Throwable) {
                        resumeWith(Result.failure(e))
                    }
                }
            }
            result
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun verifyEmail(
        context: Context,
        coroutineScope: CoroutineScope,
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

        val fragment = LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        val listenHandle = fragment.listen(authgear.core.application, this)
        fragment.waitWebViewToLoad(webViewLoadTimeoutMillis)

        val d = coroutineScope.async {
            waitForResult(listenHandle).getOrThrow()
            val userInfo = authgear.fetchUserInfo()
            userInfo
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun resetPassword(context: Context, coroutineScope: CoroutineScope, uri: Uri): Pair<Fragment, LatteHandle<Unit>> {
        val entryUrl = "$customUIEndpoint/recovery/reset"
        val redirectUri = "latte://complete"

        val resetPasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            for (q in uri.getQueryList()) {
                appendQueryParameter(q.first, q.second)
            }
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()

        val fragment = LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = resetPasswordUrl,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        val listenHandle = fragment.listen(authgear.core.application, this)
        fragment.waitWebViewToLoad(webViewLoadTimeoutMillis)

        val listener = object : LatteFragmentListener<Uri> {
            override fun onResetPasswordCompleted(resumeWith: (Result<Uri>) -> Unit) {
                val broadcastIntent = Intent(BroadcastType.RESET_PASSWORD_COMPLETED.action)
                broadcastIntent.setPackage(authgear.core.application.applicationContext.packageName)
                authgear.core.application.sendOrderedBroadcast(broadcastIntent, null)
            }
        }

        val d = coroutineScope.async {
            listenHandle.waitForResult(listener) { r, resumeWith ->
                val ruri = r.getOrThrow()
                resumeWith(Result.success(ruri))
            }
            Unit
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun changePassword(
        context: Context,
        coroutineScope: CoroutineScope,
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

        val fragment = LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        val listenHandle = fragment.listen(authgear.core.application, this)
        fragment.waitWebViewToLoad(webViewLoadTimeoutMillis)

        val d = coroutineScope.async {
            waitForResult(listenHandle).getOrThrow()
            Unit
        }

        val handle = LatteHandle(fragment.latteID, d)
        return Pair(fragment, handle)
    }

    suspend fun changeEmail(
        context: Context,
        coroutineScope: CoroutineScope,
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

        val fragment = LatteFragment.makeWithPreCreatedWebView(
            context = context,
            id = makeID(),
            url = url,
            redirectUri = redirectUri,
            webContentsDebuggingEnabled = webContentsDebuggingEnabled
        )
        val listenHandle = fragment.listen(authgear.core.application, this)
        fragment.waitWebViewToLoad(webViewLoadTimeoutMillis)

        val d = coroutineScope.async {
            waitForResult(listenHandle).getOrThrow()
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
        coroutineScope: CoroutineScope,
        callback: suspend (LatteAppLink) -> Unit
    ) {
        coroutineScope.launch {
            intents.collect {
                val uri = it?.data ?: return@collect
                val link = LatteAppLink.create(
                    uri,
                    appLinkOrigin,
                    rewriteAppLinkOrigin) ?: return@collect
                callback(link)
            }
        }
    }

    fun listenForShortLinks(
        coroutineScope: CoroutineScope,
        callback: suspend (LatteShortLink) -> Unit
    ) {
        coroutineScope.launch {
            intents.collect {
                var uri = it?.data ?: return@collect
                val origin = uri.getOrigin() ?: return@collect
                val path = uri.path ?: return@collect
                if (origin == shortLinkOrigin.getOrigin()) {
                    if (rewriteShortLinkOrigin != null) {
                        uri = uri.rewriteOrigin(rewriteShortLinkOrigin)
                    }
                    if (!path.startsWith("/s")) {
                        return@collect
                    }
                    callback(LatteShortLink(uri, appLinkOrigin, rewriteAppLinkOrigin))
                    return@collect
                }
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

    suspend fun migrateSession(
        accessToken: String,
        migrationEndpoint: String,
    ): UserInfo {
        return withContext(Dispatchers.IO) {
            val url = URL(migrationEndpoint)
            val request = SessionMigrationRequest(
                clientId = authgear.clientId,
                accessToken = accessToken,
                deviceInfo = authgear.getDeviceInfoBase64(),
            )
            val encoded = Json.encodeToString(request)

            val response: SessionMigrationResponse = HttpClient.fetch(url, "POST", emptyMap()) { conn ->
                conn.outputStream.use {
                    it.write(encoded.toByteArray(StandardCharsets.UTF_8))
                }
                conn.errorStream?.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                }
                conn.inputStream.use {
                    val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                    HttpClient.throwErrorIfNeeded(conn, responseString)
                    HttpClient.json.decodeFromString(responseString)
                }
            }

            val migrationResult = CompletableDeferred<UserInfo>()

            val listener = object : OnAuthenticateWithMigratedSessionListener {
                override fun onAuthenticated(userInfo: UserInfo) {
                    migrationResult.complete(userInfo)
                }

                override fun onAuthenticationFailed(throwable: Throwable) {
                    migrationResult.completeExceptionally(throwable)
                }
            }

            authgear.authenticateWithMigratedSession(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresIn = response.expiresIn,
                tokenType = response.tokenType,
                onAuthenticateWithMigratedSessionListener = listener,
                handler = Handler(Looper.getMainLooper()),
            )

            return@withContext migrationResult.await()
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

    private suspend fun makeAuthgearReauthenticateOptions(context: Context, latteOptions: ReauthenticateOptions): com.oursky.authgear.ReauthenticateOptions {
        val reauthXState = HashMap(latteOptions.xState)
        reauthXState["user_initiate"] = "reauth"
        val capabilities = mutableListOf<Capability>()

        val biometricOptions = latteOptions.biometricOptions
        if (biometricOptions != null) {
            val result = BiometricManager.from(context).canAuthenticate(biometricOptions.allowedAuthenticators)
            if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                capabilities.add(Capability.BIOMETRIC)
            }
        }

        reauthXState["capabilities"] = capabilities.joinToString(separator = ",") { it.raw }

        val reauthXSecrets = hashMapOf(
            "email" to latteOptions.email,
            "phone" to latteOptions.phone
        )

        val finalXState = makeXStateWithSecrets(reauthXState, reauthXSecrets)

        return ReauthenticateOptions(
            xState = finalXState.toQueryParameter(),
            redirectUri = "latte://complete",
            uiLocales = latteOptions.uiLocales
        )
    }
}
