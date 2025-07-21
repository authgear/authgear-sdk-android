package com.oursky.authgear.net

public interface HTTPClient {
    fun send(request: HTTPRequest): HTTPResponse
}
