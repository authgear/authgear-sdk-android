package com.oursky.authgear

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.oursky.authgear.data.key.KeyRepoKeystore
import com.oursky.authgear.data.oauth.OauthRepo
import com.oursky.authgear.data.token.TokenRepoInMemory
import com.oursky.authgear.oauth.ChallengeResponse
import com.oursky.authgear.oauth.OIDCConfiguration
import com.oursky.authgear.oauth.OIDCTokenRequest
import com.oursky.authgear.oauth.OIDCTokenResponse
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class AuthgearTest {
    companion object {
        private const val RefreshTokenWaitMs = 1000L

        private val ClientId = Base64.getEncoder().encodeToString(Random.nextBytes(16))
    }
    // Currently the mock is test specific, if it gains sufficient generic features, can move it
    // back to data layer.
    private class OauthRepoMock : OauthRepo {
        private val refreshedCount = AtomicInteger(0)
        override var endpoint: String? = null

        fun getRefreshedCount(): Int {
            return refreshedCount.get()
        }

        override fun getOIDCConfiguration(): OIDCConfiguration {
            return OIDCConfiguration("/authorize", "/token", "/userInfo", "/revoke", "logout")
        }

        override fun oidcTokenRequest(request: OIDCTokenRequest): OIDCTokenResponse {
            if (request.grantType == GrantType.REFRESH_TOKEN) {
                refreshedCount.accumulateAndGet(1) { a, b ->
                    a + b
                }
            }
            Thread.sleep(RefreshTokenWaitMs)
            return OIDCTokenResponse("idToken", "code", "accessToken", 0, "refreshToken")
        }

        override fun oidcRevocationRequest(refreshToken: String) {
        }

        override fun oidcUserInfoRequest(accessToken: String): UserInfo {
            return UserInfo("sub", isVerified = false, isAnonymous = false)
        }

        override fun oauthChallenge(purpose: String): ChallengeResponse {
            return ChallengeResponse("abc", "0")
        }
    }

    private lateinit var oauthMock: OauthRepoMock
    private lateinit var authgearCore: AuthgearCore

    @Before
    fun setup() {
        val application =
            InstrumentationRegistry.getInstrumentation().context.applicationContext as Application
        oauthMock = OauthRepoMock()
        authgearCore = AuthgearCore(
            application,
            ClientId,
            "EndpointNotUsed",
            TokenRepoInMemory(refreshToken = "refreshToken"),
            oauthMock,
            KeyRepoKeystore()
        )
    }

    @Test
    fun configure() {
        assertEquals(
            "ClientId is not configured properly. Client id",
            ClientId,
            authgearCore.clientId
        )
    }

    @Test(timeout = RefreshTokenWaitMs * 5)
    fun concurrentRefreshAccessTokenResultInOnlyOneRefresh() {
        runBlocking {
            authgearCore.configure(skipRefreshAccessToken = true)
            val deferredList = mutableListOf<Deferred<String?>>()
            repeat(10) {
                deferredList.add(async(Dispatchers.IO) {
                    authgearCore.refreshAccessTokenIfNeeded()
                })
            }
            deferredList.awaitAll()
            assertEquals(
                "Concurrent refresh access token not producing a single refresh request. Request count",
                1,
                oauthMock.getRefreshedCount()
            )
        }
    }

    @Test(timeout = RefreshTokenWaitMs * 10)
    fun refreshAccessTokenWorkMultipleTimes() {
        runBlocking {
            authgearCore.configure(skipRefreshAccessToken = true)
            val repeatCount = 3
            repeat(repeatCount) {
                authgearCore.refreshAccessTokenIfNeeded()
            }
            assertEquals(
                "Concurrent refresh access token not producing $repeatCount. Request count",
                repeatCount,
                oauthMock.getRefreshedCount()
            )
        }
    }
}
