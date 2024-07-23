package com.oursky.authgear.dpop

internal interface DPoPProvider {
    fun generateDPoPProof(htm: String, htu: String): String?
    fun computeJKT(): String?
}