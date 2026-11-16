package com.oursky.authgear.net

import org.junit.Assert.assertEquals
import org.junit.Test

class EncodingTest {
    @Test
    fun toQueryParameterEncodesSpaceAsPercent20() {
        assertEquals(
            "scope=openid%20offline_access",
            mapOf("scope" to "openid offline_access").toQueryParameter()
        )
    }

    @Test
    fun toFormDataEncodesSpaceAsPlus() {
        assertEquals(
            "scope=openid+offline_access",
            mapOf("scope" to "openid offline_access").toFormData()
        )
    }

    @Test
    fun literalPlusIsPercentEncoded() {
        assertEquals("a=1%2B1", mapOf("a" to "1+1").toQueryParameter())
        assertEquals("a=1%2B1", mapOf("a" to "1+1").toFormData())
    }

    @Test
    fun keysAreEncoded() {
        assertEquals("a%26b=c%3Dd", mapOf("a&b" to "c=d").toQueryParameter())
        assertEquals("a%26b=c%3Dd", mapOf("a&b" to "c=d").toFormData())
    }

    @Test
    fun noTrailingAmpersand() {
        val m = linkedMapOf("a" to "1", "b" to "2")
        assertEquals("a=1&b=2", m.toQueryParameter())
        assertEquals("a=1&b=2", m.toFormData())
    }

    @Test
    fun emptyMapEncodesToEmptyString() {
        assertEquals("", emptyMap<String, String>().toQueryParameter())
        assertEquals("", emptyMap<String, String>().toFormData())
    }
}
