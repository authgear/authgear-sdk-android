package com.oursky.authgear.data

import com.oursky.authgear.AuthgearException
import com.oursky.authgear.OAuthException
import com.oursky.authgear.ServerException
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal class HttpClient {
    companion object {
        val json = Json { ignoreUnknownKeys = true }
        fun <T> fetch(
            url: URL,
            method: String,
            headers: Map<String, String>,
            body: ByteArray? = null,
            followRedirect: Boolean = true,
            callback: (responseBody: ByteArray?) -> T
        ): T {
            var responseBody: ByteArray? = null
            var currentUrl = url
            // Follow redirects by max. 5 times
            val maxRedirects = 5
            for (i in 1..maxRedirects) {
                val conn = currentUrl.openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = method
                    conn.doInput = true
                    // We handle redirects below
                    conn.instanceFollowRedirects = false
                    headers.forEach { (key, value) ->
                        conn.setRequestProperty(key, value)
                    }
                    if (method != "GET" && method != "HEAD" && body != null) {
                        conn.doOutput = true
                        conn.outputStream.use {
                            it.write(body)
                        }
                    }
                    // Follow redirects
                    // We need this because instanceFollowRedirects do not follow redirects on POST requests
                    if (conn.responseCode in 300..399 && followRedirect) {
                        val location = conn.getHeaderField("Location")
                        val locationUtf8 = URLDecoder.decode(location, "UTF-8")
                        val next = URL(currentUrl, locationUtf8)
                        currentUrl = next
                        if (i >= maxRedirects) {
                            throw AuthgearException("maximum count of redirect reached")
                        }
                        continue
                    }

                    conn.errorStream?.use {
                        val responseString = String(it.readBytes(), StandardCharsets.UTF_8)
                        throwErrorIfNeeded(conn, responseString)
                    }
                    conn.inputStream.use {
                        val bytes = it.readBytes()
                        responseBody = bytes
                        val responseString = String(bytes, StandardCharsets.UTF_8)
                        throwErrorIfNeeded(conn, responseString)
                    }
                    break
                } finally {
                    conn.disconnect()
                }
            }
            return callback(responseBody)
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
