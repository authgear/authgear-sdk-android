package com.oursky.authgear

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind

@Serializable(with = AuthenticatorTypeSerializer::class)
enum class AuthenticatorType {
    PASSWORD,
    OOB_OTP_EMAIL,
    OOB_OTP_SMS,
    TOTP,
    UNKNOWN
}

object AuthenticatorTypeSerializer : KSerializer<AuthenticatorType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AuthenticatorType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AuthenticatorType {
        val value = decoder.decodeString()
        return when (value) {
            "password" -> AuthenticatorType.PASSWORD
            "oob_otp_email" -> AuthenticatorType.OOB_OTP_EMAIL
            "oob_otp_sms" -> AuthenticatorType.OOB_OTP_SMS
            "totp" -> AuthenticatorType.TOTP
            else -> AuthenticatorType.UNKNOWN
        }
    }

    override fun serialize(encoder: Encoder, value: AuthenticatorType) {
        val stringValue = when (value) {
            AuthenticatorType.PASSWORD -> "password"
            AuthenticatorType.OOB_OTP_EMAIL -> "oob_otp_email"
            AuthenticatorType.OOB_OTP_SMS -> "oob_otp_sms"
            AuthenticatorType.TOTP -> "totp"
            AuthenticatorType.UNKNOWN -> "unknown"
        }
        encoder.encodeString(stringValue)
    }
}
