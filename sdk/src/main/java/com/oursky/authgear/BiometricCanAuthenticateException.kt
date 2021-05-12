package com.oursky.authgear

class BiometricCanAuthenticateException(val result: Int) : AuthgearException(canAuthenticateResultToString(result))