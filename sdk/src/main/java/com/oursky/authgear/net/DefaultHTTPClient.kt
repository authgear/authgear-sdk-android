package com.oursky.authgear.net

import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection

class DefaultHTTPClient: HTTPClient {
    override fun send(request: HTTPRequest): HTTPResponse {
        val followRedirect = request.followRedirect ?: true
        val url = request.uri.toURL()
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = request.method
            conn.instanceFollowRedirects = followRedirect
            conn.doInput = true

            if (conn.requestMethod != "GET" && conn.requestMethod != "HEAD") {
                conn.doOutput = true
            }

            for (entry in request.headers) {
                for (value in entry.value) {
                    conn.addRequestProperty(entry.key, value)
                }
            }

            if (conn.doOutput) {
                request.body?.let {
                    this.transferTo(it, conn.outputStream)
                }
            }

            val statusCode = conn.responseCode
            val responseHeaders = conn.headerFields
            val responseBody: InputStream
            if (conn.errorStream != null) {
                responseBody = conn.errorStream
            } else {
                responseBody = conn.inputStream
            }

            val response = HTTPResponse(statusCode, responseHeaders, responseBody)
            return response
        } finally {
            conn.disconnect()
        }
    }

    private fun transferTo(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(8192)
        var totalBytes = 0L
        var bytesRead: Int

        while (true) {
            bytesRead = input.read(buffer)
            if (bytesRead == -1) {
                break
            }
            output.write(buffer, 0, bytesRead)
            totalBytes += bytesRead
        }

        return totalBytes
    }
}