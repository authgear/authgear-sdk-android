package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UserInfo(
    val sub: String,
    @SerialName("https://authgear.com/claims/user/is_verified")
    val isVerified: Boolean,
    @SerialName("https://authgear.com/claims/user/is_anonymous")
    val isAnonymous: Boolean,
    @SerialName("https://authgear.com/claims/user/can_reauthenticate")
    val canReauthenticate: Boolean,
    @SerialName("https://authgear.com/claims/user/has_primary_password")
    val hasPrimaryPassword: Boolean,
    @SerialName("https://authgear.com/claims/user/roles")
    val roles: Array<String>? = null,
    @SerialName("custom_attributes")
    val customAttributes: JsonObject,
    val email: String? = null,
    @SerialName("email_verified")
    val emailVerified: Boolean? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("phone_number_verified")
    val phoneNumberVerified: Boolean? = null,
    @SerialName("preferred_username")
    val preferredUsername: String? = null,
    @SerialName("family_name")
    val familyName: String? = null,
    @SerialName("given_name")
    val givenName: String? = null,
    @SerialName("middle_name")
    val middleName: String? = null,
    val name: String? = null,
    val nickname: String? = null,
    val picture: String? = null,
    val profile: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    val zoneinfo: String? = null,
    val locale: String? = null,
    val address: UserInfoAddress? = null
)
