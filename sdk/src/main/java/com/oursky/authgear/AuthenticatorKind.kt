package com.oursky.authgear

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind

@Serializable(with = AuthenticatorKindSerializer::class)
enum class AuthenticatorKind {
    PRIMARY,
    SECONDARY,
    UNKNOWN
}

object AuthenticatorKindSerializer : KSerializer<AuthenticatorKind> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AuthenticatorKind", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AuthenticatorKind {
        val value = decoder.decodeString()
        return when (value) {
            "primary" -> AuthenticatorKind.PRIMARY
            "secondary" -> AuthenticatorKind.SECONDARY
            else -> AuthenticatorKind.UNKNOWN
        }
    }

    override fun serialize(encoder: Encoder, value: AuthenticatorKind) {
        val stringValue = when (value) {
            AuthenticatorKind.PRIMARY -> "primary"
            AuthenticatorKind.SECONDARY -> "secondary"
            AuthenticatorKind.UNKNOWN -> "unknown"
        }
        encoder.encodeString(stringValue)
    }
}
