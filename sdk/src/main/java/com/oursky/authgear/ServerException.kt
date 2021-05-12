package com.oursky.authgear

import org.json.JSONObject

class ServerException : AuthgearException {
    val name: String
    val reason: String
    var info: JSONObject?

    constructor(name: String, reason: String, message: String, info: JSONObject?) : super(message) {
        this.name = name
        this.reason = reason
        this.info = info
    }
}