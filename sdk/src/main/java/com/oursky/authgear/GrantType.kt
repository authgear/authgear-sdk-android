package com.oursky.authgear

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = GrantTypeSerializer :: class)
enum class GrantType(val raw: String) {
    AUTHORIZATION_CODE("authorization_code"),
    REFRESH_TOKEN("refresh_token"),
    ANONYMOUS("urn:authgear:params:oauth:grant-type:anonymous-request"),
    BIOMETRIC("urn:authgear:params:oauth:grant-type:biometric-request"),
    ID_TOKEN("urn:authgear:params:oauth:grant-type:id-token"),
    APP2APP("urn:authgear:params:oauth:grant-type:app2app-request"),
    SETTINGS_ACTION("urn:authgear:params:oauth:grant-type:settings-action"),
    TOKEN_EXCHANGE("urn:ietf:params:oauth:grant-type:token-exchange")
}

@Serializer(forClass = GrantType::class)
object GrantTypeSerializer : KSerializer<GrantType> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "grant_type",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: GrantType) {
        encoder.encodeString(value.raw)
    }

    override fun deserialize(decoder: Decoder): GrantType {
        return decoder.decodeString().let { decodedString ->
            // throw if not found
            GrantType.values().firstOrNull { decodedString == it.raw }
                ?: run {
                    throw SerializationException("Could not deserialize grant_type, enum member not found")
                }
        }
    }
}