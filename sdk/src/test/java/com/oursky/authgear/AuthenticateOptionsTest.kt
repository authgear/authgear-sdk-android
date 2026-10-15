package com.oursky.authgear

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticateOptionsTest {
    @Test
    fun getScopesReturnsScopesRequiredBySdk() {
        assertEquals(
            listOf("openid", "offline_access", "https://authgear.com/scopes/full-access"),
            AuthenticateOptions.getScopes(false)
        )
    }

    @Test
    fun getScopesMergesAdditionalScopes() {
        assertEquals(
            listOf(
                "openid",
                "offline_access",
                "https://authgear.com/scopes/full-access",
                "device_sso",
                "https://authgear.com/scopes/pre-authenticated-url",
                "email",
                "profile"
            ),
            AuthenticateOptions.getScopes(true, listOf("email", "profile"))
        )
    }

    @Test
    fun getScopesNeverDropsOrDuplicatesScopesRequiredBySdk() {
        assertEquals(
            listOf("openid", "offline_access", "https://authgear.com/scopes/full-access", "email"),
            AuthenticateOptions.getScopes(false, listOf("email", "openid", "email"))
        )
    }

    @Test
    fun toRequestIncludesAdditionalScopes() {
        val request = AuthenticateOptions(redirectUri = "com.example://host/path", scopes = listOf("email"))
            .toRequest(isSsoEnabled = false, preAuthenticatedURLEnabled = false, dpopJKT = null)
        assertEquals(
            listOf("openid", "offline_access", "https://authgear.com/scopes/full-access", "email"),
            request.scope
        )
    }
}
