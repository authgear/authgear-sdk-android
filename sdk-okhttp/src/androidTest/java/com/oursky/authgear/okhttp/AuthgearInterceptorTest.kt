package com.oursky.authgear.okhttp

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oursky.authgear.Authgear
import com.oursky.authgear.ConfigureOptions
import com.oursky.authgear.configure
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request

import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AuthgearInterceptorTest {
    @Test
    fun useAppContext() {
        val application =
            InstrumentationRegistry.getInstrumentation().context.applicationContext as Application
        val authgear = Authgear(application)
        // TODO: Can't mock authgear as this is a standalone package so this would fail.
        // Need to start up a lightweight server and fake the whole oauth flow.
        runBlocking {
            authgear.configure(ConfigureOptions("clientId", "http://localhost:3000"))
        }
        val accessToken = authgear.accessToken
        val testInterceptor = Interceptor {
            val bearerToken = it.request().header("authorization")
            assert(bearerToken != null && bearerToken == "bearer $accessToken")
            it.proceed(it.request())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthgearInterceptor(authgear))
            .addInterceptor(testInterceptor)
            .build()
        client.newCall(
            Request.Builder().url("http://localhost:3000")
                .get().build()
        ).execute()
    }
}