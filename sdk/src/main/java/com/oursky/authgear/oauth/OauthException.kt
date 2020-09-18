package com.oursky.authgear.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.Exception

@Serializable
data class OauthException(
    @SerialName("error")
    val error: String,
    @SerialName("error_description")
    val errorDescription: String
) : Exception()
