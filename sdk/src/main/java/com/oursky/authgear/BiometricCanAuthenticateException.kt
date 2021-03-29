package com.oursky.authgear

import java.lang.RuntimeException

class BiometricCanAuthenticateException(val result: Int) : RuntimeException(canAuthenticateResultToString(result))