package com.irnova.novaradar.data.repository

import com.irnova.novaradar.data.model.ScanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerRepository @Inject constructor() {
    
    private val _scanResults = MutableSharedFlow<ScanResult>()
    val scanResults = _scanResults.asSharedFlow()

    private var scanJob: Job? = null

    fun startScan(subnet: String, port: Int, timeout: Int = 500) {
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            // منطق نمونه برای اسکن رنج 1 تا 255 (قابل توسعه به رنج‌های سفارشی)
            for (i in 1..255) {
                if (!isActive) break
                val targetIp = "$subnet.$i"
                
                launch {
                    val latency = checkTcpConnection(targetIp, port, timeout)
                    if (latency >= 0) {
                        _scanResults.emit(
                            ScanResult(
                                ip = targetIp,
                                port = port,
                                latencyMs = latency,
                                link = "$targetIp:$port"
                            )
                        )
                    }
                }
                delay(10) // کنترل نرخ اسکن برای جلوگیری از فشار به سیستم
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    private fun checkTcpConnection(ip: String, port: Int, timeout: Int): Long {
        val startTime = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                System.currentTimeMillis() - startTime
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
