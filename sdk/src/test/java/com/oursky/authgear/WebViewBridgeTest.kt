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
        val reply = parse(capabilitiesReply("1"))
        assertEquals(JsonPrimitive("1"), reply["id"])
        assertEquals(JsonPrimitive("ok"), reply["status"])
        assertEquals(
            JsonArray(BRIDGE_CAPABILITIES.map { JsonPrimitive(it) }),
            reply["capabilities"]
        )
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
    fun replyWithEachStatus() {
        for (status in listOf("ok", "unsupported", "error")) {
            assertEquals(
                parse("""{"id":"7","status":"$status"}"""),
                parse(reply("7", status))
            )
        }
    }

    @Test
    fun replyWithMessage() {
        assertEquals(
            parse("""{"id":"7","status":"error","message":"boom"}"""),
            parse(reply("7", "error", "boom"))
        )
    }
}
