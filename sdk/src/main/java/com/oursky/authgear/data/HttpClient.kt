package com.oursky.authgear.data

import com.oursky.authgear.AuthgearException
import com.oursky.authgear.OAuthException
import com.oursky.authgear.ServerException
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class HttpClient {
    companion object {
        val json = Json { ignoreUnknownKeys = true }
        fun <T> fetch(
            url: URL,
            method: String,
            headers: Map<String, String>,
            followRedirect: Boolean = true,
            callback: (conn: HttpURLConnection) -> T
        ): T {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.doInput = true

                if (!followRedirect) {
                    conn.instanceFollowRedirects = false
                }

                if (method != "GET" && method != "HEAD") {
                    conn.doOutput = true
                }
                headers.forEach { (key, value) ->
                    conn.setRequestProperty(key, value)
                }
                return callback(conn)
            } finally {
                conn.disconnect()
            }
        }

        fun throwErrorIfNeeded(conn: HttpURLConnection, responseString: String) {
            if (conn.responseCode < 200 || conn.responseCode >= 300) {
                try {
                    val jsonObject = JSONObject(responseString)
                    val e = makeError(jsonObject)
                    if (e != null) {
                        throw e
                    }
                    throw AuthgearException(responseString)
                } catch (e: JSONException) {
                    throw AuthgearException(responseString, e)
                }
            }
        }

        fun makeError(jsonObject: JSONObject): Exception? {
            if (jsonObject.has("error")) {
                val any = jsonObject.get("error")
                if (any is JSONObject) {
                    if (any.has("name") && any.has("reason") && any.has("message")) {
                        return ServerException(any)
                    }
                }
                if (any is String) {
                    var state: String? = null
                    var errorDescription: String? = null
                    var errorURI: String? = null
                    if (jsonObject.has("state")) {
                        state = jsonObject.getString("state")
                    }
                    if (jsonObject.has("error_description")) {
                        errorDescription = jsonObject.getString("error_description")
                    }
                    if (jsonObject.has("error_uri")) {
                        errorURI = jsonObject.getString("error_uri")
                    }
                    return OAuthException(
                        error = jsonObject.getString("error"),
                        errorDescription = errorDescription,
                        state = state,
                        errorURI = errorURI
                    )
                }
            }
            return null
        }
    }
}
