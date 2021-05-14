package com.oursky.authgear

class BiometricPromptAuthenticationException(val errorCode: Int) : AuthgearException(authenticateErrorCodeToString(errorCode))