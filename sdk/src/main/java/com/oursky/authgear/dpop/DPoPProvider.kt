package com.oursky.authgear.dpop

interface DPoPProvider {
    fun generateDPoPProof(htm: String, htu: String): String?
    fun computeJKT(): String?
}