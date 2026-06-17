package com.irnova.novaradar.data.model

data class ScanStats(
    val scanning: Boolean = false,
    val currentIP: String = "0.0.0.0",
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val totalScanned: Int = 0,
    val progress: Float = 0f
)
