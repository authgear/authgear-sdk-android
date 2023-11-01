package com.oursky.authgear.data.assetlink

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AssetLink(
    val relation: List<String>,
    val target: Target
) {
    @Serializable
    internal data class Target(
        @SerialName("namespace")
        val namespace: String,
        @SerialName("package_name")
        val packageName: String,
        @SerialName("sha256_cert_fingerprints")
        val sha256CertFingerprints: List<String>
    )
}
