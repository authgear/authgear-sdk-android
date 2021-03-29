package com.oursky.authgear

import java.lang.RuntimeException

class BiometricPromptAuthenticationException(val errorCode: Int) : RuntimeException(authenticateErrorCodeToString(errorCode))