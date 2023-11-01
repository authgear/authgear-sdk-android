package com.oursky.authgear.latte

import android.net.Uri
import android.util.Base64
import com.oursky.authgear.CancelException
import com.oursky.authgear.OAuthException
import com.oursky.authgear.ServerException
import kotlinx.serialization.Serializable
import org.json.JSONObject

@Serializable
internal data class LatteResult(
    val errorMessage: String? = null,
    val finishUri: String? = null,
    val isCancel: Boolean = false
) {
    fun getOrThrow(): Uri {
        if (isCancel) {
            throw CancelException()
        }
        if (!errorMessage.isNullOrBlank()) {
            throw RuntimeException(errorMessage)
        }
        val uri = Uri.parse(finishUri)
        val error = uri.getQueryParameter("error")
        if (error == "cancel") {
            throw CancelException()
        }

        val latteError = uri.getQueryParameter("x_latte_error")
        latteError?.let { base64Json ->
            val json = Base64.decode(base64Json, Base64.URL_SAFE).toString(Charsets.UTF_8)
            throw ServerException(JSONObject(json))
        }

        if (!error.isNullOrBlank()) {
            throw OAuthException(
                error = error,
                errorDescription = uri.getQueryParameter("error_description"),
                state = null,
                errorURI = uri.getQueryParameter("error_uri")
            )
        }

        return uri
    }
}
