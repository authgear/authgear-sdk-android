package com.oursky.authgear.data

import com.oursky.authgear.net.toFormData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class HttpClient {
    companion object {
        val json = Json { ignoreUnknownKeys = true }
        inline fun <reified T> getJson(url: URL, headers: Map<String, String>? = null): T {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                headers?.forEach { (key, value) ->
                    conn.setRequestProperty(key, value)
                }
                conn.inputStream.use {
                    return json.decodeFromString(String(it.readBytes(), StandardCharsets.UTF_8))
                }
            } finally {
                conn.disconnect()
            }
        }
        inline fun <reified R, reified E : Exception> postFormRespJsonWithError(url: URL, body: MutableMap<String, String>): R {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("content-type", "application/x-www-form-urlencoded")
                conn.outputStream.use {
                    it.write(body.toFormData().toByteArray(StandardCharsets.UTF_8))
                }
                conn.inputStream.use {
                    return json.decodeFromString(String(it.readBytes(), StandardCharsets.UTF_8))
                }
            } catch (e: Exception) {
                conn.errorStream.use {
                    try {
                        val errorResp: E = json.decodeFromString(String(it.readBytes(), StandardCharsets.UTF_8))
                        throw errorResp
                    } catch (innerE: Exception) {
                        throw innerE
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
        fun postForm(url: URL, body: MutableMap<String, String>) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("content-type", "application/x-www-form-urlencoded")
                conn.outputStream.use {
                    it.write(body.toFormData().toByteArray(StandardCharsets.UTF_8))
                }
                conn.connect()
            } finally {
                conn.disconnect()
            }
        }
    }
}
