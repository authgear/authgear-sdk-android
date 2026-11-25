package com.oursky.authgear

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class WebViewBridgeTest {
    private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun bridgeAllowedOriginAllowsHttps() {
        assertTrue(isBridgeAllowedOrigin("https://ui.example.com"))
        assertTrue(isBridgeAllowedOrigin("https://ui.example.com:8443/login?x=1"))
        assertTrue(isBridgeAllowedOrigin("HTTPS://UI.EXAMPLE.COM"))
    }

    @Test
    fun bridgeAllowedOriginAllowsHttpOnlyForLocalhost() {
        assertTrue(isBridgeAllowedOrigin("http://localhost:8300/login"))
        assertFalse(isBridgeAllowedOrigin("http://ui.example.com"))
        assertFalse(isBridgeAllowedOrigin("http://127.0.0.1:8300"))
    }

    @Test
    fun bridgeAllowedOriginRejectsOtherURLs() {
        assertFalse(isBridgeAllowedOrigin("myapp://host"))
        assertFalse(isBridgeAllowedOrigin("ui.example.com"))
        assertFalse(isBridgeAllowedOrigin("https://"))
        assertFalse(isBridgeAllowedOrigin("https://bad host"))
    }

    @Test
    fun allowedOriginRuleWithoutPort() {
        assertEquals("https://auth.example.com", allowedOriginRule(URI("https://auth.example.com")))
    }

    @Test
    fun allowedOriginRuleWithExplicitPort() {
        assertEquals("http://localhost:3000", allowedOriginRule(URI("http://localhost:3000")))
    }

    @Test
    fun allowedOriginRuleDropsUserInfo() {
        assertEquals(
            "https://auth.example.com:8443",
            allowedOriginRule(URI("https://user:pass@auth.example.com:8443"))
        )
    }

    @Test
    fun allowedOriginRuleDropsPathAndQuery() {
        assertEquals(
            "https://auth.example.com",
            allowedOriginRule(URI("https://auth.example.com/oauth2/authorize?client_id=a#b"))
        )
    }

    @Test
    fun credentialManagerRequestJsonKeepsOnlyTheSpecFields() {
        val publicKey = parse(
            """{"challenge":"c","rpId":"auth.example.com","userVerification":"preferred",""" +
                """"allowCredentials":[{"type":"public-key","id":"a"}],""" +
                """"timeout":60000,"hints":["security-key"],"extensions":{"prf":{}}}"""
        )
        assertEquals(
            parse(
                """{"challenge":"c","rpId":"auth.example.com","userVerification":"preferred",""" +
                    """"allowCredentials":[{"type":"public-key","id":"a"}]}"""
            ),
            credentialManagerRequestJson(publicKey)
        )
    }

    @Test
    fun credentialManagerRequestJsonDropsEmptyAllowCredentials() {
        val publicKey = parse("""{"challenge":"c","rpId":"auth.example.com","allowCredentials":[]}""")
        assertEquals(
            parse("""{"challenge":"c","rpId":"auth.example.com"}"""),
            credentialManagerRequestJson(publicKey)
        )
    }

    @Test
    fun allowedOriginRuleWithoutHost() {
        // java.net.URI has no host for a hostname with "_".
        assertNull(allowedOriginRule(URI("https://my_app.example.com")))
        assertNull(allowedOriginRule(URI("auth.example.com")))
    }

    @Test
    fun sameOriginEqual() {
        assertTrue(sameOrigin(URI("https://auth.example.com/login"), URI("https://auth.example.com")))
    }

    @Test
    fun sameOriginDifferentHost() {
        assertFalse(sameOrigin(URI("https://auth.example.com"), URI("https://example.com")))
    }

    @Test
    fun sameOriginDifferentScheme() {
        assertFalse(sameOrigin(URI("https://auth.example.com"), URI("http://auth.example.com")))
    }

    @Test
    fun sameOriginDifferentPort() {
        assertFalse(sameOrigin(URI("https://auth.example.com:8443"), URI("https://auth.example.com")))
    }

    @Test
    fun sameOriginExplicitDefaultPortEqualsNoPort() {
        assertTrue(sameOrigin(URI("https://auth.example.com:443"), URI("https://auth.example.com")))
        assertTrue(sameOrigin(URI("http://localhost"), URI("http://localhost:80")))
    }

    @Test
    fun capabilitiesReplyListsTheCapabilities() {
        val reply = parse(capabilitiesReply("1").encode())
        assertEquals(JsonPrimitive("1"), reply["id"])
        assertEquals(JsonPrimitive("ok"), reply["status"])
        assertEquals(
            JsonArray(
                listOf(
                    JsonPrimitive("credentials.get"),
                    JsonPrimitive("credentials.get:publicKey"),
                    JsonPrimitive("credentials.get:immediate")
                )
            ),
            reply["capabilities"]
        )
    }

    @Test
    fun isSameOriginOfStrings() {
        assertTrue(isSameOrigin("https://auth.example.com/login?x=1", "https://auth.example.com"))
        assertFalse(isSameOrigin("https://other.example.com/", "https://auth.example.com"))
        assertFalse(isSameOrigin(null, "https://auth.example.com"))
        assertFalse(isSameOrigin("https://auth.example.com", null))
        assertFalse(isSameOrigin("https://auth example.com", "https://auth.example.com"))
        assertFalse(isSameOrigin("about:blank", "https://auth.example.com"))
    }

    @Test
    fun parseRequestValid() {
        val request = parseRequest("""{"id":"1","type":"bridge.capabilities","extra":true}""")!!
        assertEquals("1", request.id)
        assertEquals("bridge.capabilities", request.type)
        assertEquals(JsonPrimitive(true), request.json["extra"])
    }

    @Test
    fun parseRequestWithoutType() {
        val request = parseRequest("""{"id":"1"}""")!!
        assertEquals("1", request.id)
        assertNull(request.type)
    }

    @Test
    fun parseRequestNotJson() {
        assertNull(parseRequest("not json"))
    }

    @Test
    fun parseRequestJsonArray() {
        assertNull(parseRequest("""[{"id":"1"}]"""))
    }

    @Test
    fun parseRequestMissingId() {
        assertNull(parseRequest("""{"type":"bridge.capabilities"}"""))
    }

    @Test
    fun parseRequestNonStringId() {
        assertNull(parseRequest("""{"id":1,"type":"bridge.capabilities"}"""))
        assertNull(parseRequest("""{"id":null,"type":"bridge.capabilities"}"""))
    }

    @Test
    fun statusesEncodeToTheirWireStrings() {
        assertEquals(
            mapOf(
                BridgeStatus.OK to "ok",
                BridgeStatus.UNSUPPORTED to "unsupported",
                BridgeStatus.ERROR to "error",
                BridgeStatus.NO_CREDENTIAL to "no_credential",
                BridgeStatus.CANCELLED to "cancelled"
            ),
            BridgeStatus.values().associateWith { it.value }
        )
        for (status in BridgeStatus.values()) {
            assertEquals(
                parse("""{"id":"7","status":"${status.value}"}"""),
                parse(BridgeReply("7", status).encode())
            )
        }
    }

    @Test
    fun replyOmitsNullFields() {
        val reply = parse(BridgeReply("7", BridgeStatus.ERROR).encode())
        assertEquals(setOf("id", "status"), reply.keys)
    }

    @Test
    fun replyWithMessage() {
        assertEquals(
            parse("""{"id":"7","status":"error","message":"boom"}"""),
            parse(BridgeReply("7", BridgeStatus.ERROR, "boom").encode())
        )
    }

    private val credentialText = """
        {
          "id": "dFcL6B0cTujk-mONTRqsP4TXVrLWWvzWfa7oG_b36T8",
          "rawId": "dFcL6B0cTujk-mONTRqsP4TXVrLWWvzWfa7oG_b36T8",
          "type": "public-key",
          "authenticatorAttachment": "platform",
          "response": {
            "clientDataJSON": "Y2xpZW50RGF0YQ",
            "authenticatorData": "YXV0aERhdGE",
            "signature": "c2lnbmF0dXJl",
            "userHandle": "dXNlcg"
          },
          "clientExtensionResults": {}
        }
    """

    @Test
    fun okReplyCarriesTheParsedCredential() {
        val credential = BridgePublicKeyCredential.parse(credentialText)!!
        assertEquals(
            parse("""{"id":"2","status":"ok","credential":$credentialText}"""),
            parse(BridgeReply("2", BridgeStatus.OK, credential = credential).encode())
        )
    }

    @Test
    fun publicKeyCredentialWithoutOptionalFields() {
        val text = """
            {
              "id": "a", "rawId": "a", "type": "public-key",
              "response": { "clientDataJSON": "b", "authenticatorData": "c", "signature": "d" }
            }
        """
        val credential = BridgePublicKeyCredential.parse(text)!!
        assertNull(credential.authenticatorAttachment)
        assertNull(credential.response.userHandle)
        assertEquals(
            parse(
                """
                {
                  "id": "a", "rawId": "a", "type": "public-key",
                  "response": { "clientDataJSON": "b", "authenticatorData": "c", "signature": "d" },
                  "clientExtensionResults": {}
                }
                """
            ),
            credential.toJson()
        )
    }

    @Test
    fun publicKeyCredentialParseRejectsMissingFields() {
        assertNull(BridgePublicKeyCredential.parse("not json"))
        assertNull(BridgePublicKeyCredential.parse("[]"))
        assertNull(BridgePublicKeyCredential.parse("""{"rawId":"a","response":{}}"""))
        assertNull(
            BridgePublicKeyCredential.parse(
                """{"id":"a","rawId":"a","response":{"clientDataJSON":"b","authenticatorData":"c"}}"""
            )
        )
        assertNull(
            BridgePublicKeyCredential.parse(
                """{"id":1,"rawId":"a","response":{"clientDataJSON":"b","authenticatorData":"c","signature":"d"}}"""
            )
        )
    }

    @Test
    fun isRpIdAllowedWhenEqualToHost() {
        assertTrue(isRpIdAllowed("auth.example.com", "auth.example.com"))
    }

    @Test
    fun isRpIdAllowedForParentDomain() {
        assertTrue(isRpIdAllowed("example.com", "auth.example.com"))
    }

    @Test
    fun isRpIdAllowedIgnoresCase() {
        assertTrue(isRpIdAllowed("Example.COM", "AUTH.example.com"))
    }

    @Test
    fun isRpIdAllowedRejectsSiblingDomain() {
        assertFalse(isRpIdAllowed("other.example.com", "auth.example.com"))
    }

    @Test
    fun isRpIdAllowedRejectsSuffixNotAtLabelBoundary() {
        assertFalse(isRpIdAllowed("example.com", "evilexample.com"))
    }

    @Test
    fun isRpIdAllowedRejectsRpIdLongerThanHost() {
        assertFalse(isRpIdAllowed("auth.example.com", "example.com"))
    }

    @Test
    fun isRpIdAllowedRejectsEmptyRpId() {
        assertFalse(isRpIdAllowed("", "example.com"))
    }
}
