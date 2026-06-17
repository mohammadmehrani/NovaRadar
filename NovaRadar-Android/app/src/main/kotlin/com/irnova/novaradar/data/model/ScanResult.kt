package com.irnova.novaradar.data.model

data class ScanResult(
    val ip: String,
    val port: Int,
    val latencyMs: Long,
    val link: String, // مثلاً vless://... یا فقط IP:Port
    val isAlive: Boolean = true
)
