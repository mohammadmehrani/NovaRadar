package com.irnova.novaradar.data.model

data class ScanStats(
    val scanning: Boolean = false,
    val currentIP: String = "0.0.0.0",
    val currentPort: Int = 0,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val totalScanned: Int = 0,
    val totalToScan: Int = 0,
    val isSecondPass: Boolean = false,
    val progress: Float = 0f
)
