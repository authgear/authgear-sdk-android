package com.oursky.authgear

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AuthenticatorType {
    @SerialName("password")
    PASSWORD,
    @SerialName("oob_otp_email")
    OOB_OTP_EMAIL,
    @SerialName("oob_otp_sms")
    OOB_OTP_SMS,
    @SerialName("totp")
    TOTP
}
