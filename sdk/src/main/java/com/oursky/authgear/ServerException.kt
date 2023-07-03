package com.oursky.authgear

import org.json.JSONObject

class ServerException(
    val name: String,
    val reason: String,
    message: String,
    val info: JSONObject? = null
) : AuthgearException(message) {
    constructor(json: JSONObject) : this(
        json.optString("name"),
        json.optString("reason"),
        json.optString("message"),
        json.optJSONObject("info")
    )
}