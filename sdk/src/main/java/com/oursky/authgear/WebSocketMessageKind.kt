package com.oursky.authgear

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WebSocketMessageKindSerializer:: class)
internal enum class WebSocketMessageKind(val raw: String) {
    REFRESH("refresh"),
    WECHAT_LOGIN_START("wechat_login_start"),
    UNKNOWN("")
}

@Serializer(forClass = WebSocketMessageKind::class)
internal object WebSocketMessageKindSerializer : KSerializer<WebSocketMessageKind> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "web_socket_message_kind",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: WebSocketMessageKind) {
        encoder.encodeString(value.raw)
    }

    override fun deserialize(decoder: Decoder): WebSocketMessageKind {
        return decoder.decodeString().let { decodedString ->
            WebSocketMessageKind.values().firstOrNull { decodedString == it.raw }
                ?: WebSocketMessageKind.UNKNOWN
        }
    }
}
