package com.oursky.authgear

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import kotlin.random.Random

class AuthgearTest {
    @Test
    fun configure() {
        runBlocking {
            val clientId = Base64.getEncoder().encodeToString(Random.nextBytes(16))
            val application = InstrumentationRegistry.getInstrumentation().context.applicationContext as Application
            val authgear = Authgear(application)
            authgear.configure(ConfigureOptions(clientId, "NotUsed"))
            assert(authgear.clientId == clientId) { "ClientId is not configured properly" }
        }
    }
}
