package com.oursky.authgear.latte

import android.net.Uri
import android.util.Base64
import com.oursky.authgear.AuthgearException
import com.oursky.authgear.CancelException
import com.oursky.authgear.ServerException
import org.json.JSONObject
internal data class WebViewResult(private val finishUri: Uri) {
    fun getOrThrow(): Uri {
        val error = finishUri.getQueryParameter("error")
        if (error == "cancel") {
            throw CancelException()
        }

        val latteError = finishUri.getQueryParameter("x_latte_error")
        latteError?.let { base64Json ->
            val json = Base64.decode(base64Json, Base64.URL_SAFE).toString(Charsets.UTF_8)
            throw ServerException(JSONObject(json))
        }

        if (!error.isNullOrBlank()) {
            throw AuthgearException(error)
        }

        return finishUri
    }
}