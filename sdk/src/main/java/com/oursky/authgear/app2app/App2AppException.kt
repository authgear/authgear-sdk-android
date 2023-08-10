package com.oursky.authgear.app2app

import android.net.Uri
import com.oursky.authgear.AuthgearException

sealed class App2AppException(message: String) : AuthgearException(message = message) {
    class PackageNotFoundException(uri: Uri) : App2AppException(
        "No package found to handle uri: $uri")
    class PackageIntegrityException(packageName: String) : App2AppException(
        "package integrity cannot be verified: $packageName")
}