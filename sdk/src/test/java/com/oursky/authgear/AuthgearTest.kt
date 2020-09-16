package com.oursky.authgear

import org.junit.Test
import java.util.*
import kotlin.random.Random

class AuthgearTest {
    @Test
    fun configure() {
        val clientId = Base64.getEncoder().encodeToString(Random.nextBytes(16))
        val authgear = Authgear()
        authgear.configure(ConfigureOptions(clientId, "NotUsed"))
        assert(authgear.clientId == clientId) { "ClientId is not configured properly" }
    }
}
