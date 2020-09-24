package com.oursky.authgear.okhttp

import com.oursky.authgear.Authgear
import okhttp3.Interceptor
import okhttp3.Response

class AuthgearInterceptor(private val authgear: Authgear) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = authgear.refreshAccessTokenIfNeededSync()
        return chain.proceed(chain.request().newBuilder()
            .addHeader("authorization", "bearer ${accessToken ?: ""}")
            .build())
    }
}
