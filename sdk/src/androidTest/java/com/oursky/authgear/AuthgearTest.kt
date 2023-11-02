package com.oursky.authgear

import com.oursky.authgear.oauth.OidcTokenRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthgearTest {

    @Before
    fun setup() {
    }

    @Test
    fun serializeGrantType() {
        val testOidcTokenRequest = OidcTokenRequest(
            grantType = GrantType.ANONYMOUS,
            clientId = "test",
            xDeviceInfo = "dummy"
        )
        val encoded = Json.encodeToString(
            testOidcTokenRequest
        )
        assertEquals(
            "Encoded OidcTokenRequest does not match with expected JSON string",
            "{\"grant_type\":\"urn:authgear:params:oauth:grant-type:anonymous-request\"," +
                "\"client_id\":\"test\",\"x_device_info\":\"dummy\"}",
            encoded
        )
        val decoded: OidcTokenRequest = Json.decodeFromString(encoded)
        assertEquals(
            "Expect serializing and deserializing with recover the same object",
            decoded,
            testOidcTokenRequest
        )
    }
}
