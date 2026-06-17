package com.irnova.novaradar.data.repository

import com.irnova.novaradar.data.model.ScanResult
import com.irnova.novaradar.data.model.ScanStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

@Singleton
class ScannerRepository @Inject constructor() {

    private val _scanResults = MutableSharedFlow<ScanResult>()
    val scanResults = _scanResults.asSharedFlow()

    private val _scanStats = MutableStateFlow(ScanStats())
    val scanStats = _scanStats.asStateFlow()

    private var scanJob: Job? = null
    private val client = OkHttpClient()

    private val tlsPorts = setOf(443, 2053, 2083, 2087, 2096, 8443)
    private val fallbackCIDRs = listOf(
        "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
        "141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
        "197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
        "104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22"
    )

    fun startScan() {
        scanJob?.cancel()
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            _scanStats.value = ScanStats(scanning = true)
            
            val ips = mutableListOf<String>()
            // Simplification: generate some IPs from fallback CIDRs for now
            // In Go code it fetches from URLs, we can add that later if needed
            fallbackCIDRs.forEach { cidr ->
                ips.addAll(generateRandomIPs(cidr, 20))
            }
            
            if (ips.isEmpty()) {
                _scanStats.value = _scanStats.value.copy(scanning = false)
                return@launch
            }

            val ports = listOf(443, 8443)
            val total = ips.size * ports.size * 2 // 2 passes
            _scanStats.value = _scanStats.value.copy(totalToScan = total)

            // Quick Scan
            val candidates = mutableListOf<ScanResult>()
            val quickScanJobs = ips.flatMap { ip ->
                ports.map { port ->
                    async {
                        if (!isActive) return@async
                        val latency = checkTcpConnection(ip, port, 2000)
                        _scanStats.value = _scanStats.value.copy(
                            totalScanned = _scanStats.value.totalScanned + 1,
                            currentIP = ip,
                            currentPort = port
                        )
                        if (latency >= 0) {
                            val result = ScanResult(ip, port, latency, "$ip:$port")
                            synchronized(candidates) { candidates.add(result) }
                            _scanStats.value = _scanStats.value.copy(aliveCount = _scanStats.value.aliveCount + 1)
                        } else {
                            _scanStats.value = _scanStats.value.copy(deadCount = _scanStats.value.deadCount + 1)
                        }
                    }
                }
            }
            quickScanJobs.awaitAll()

            if (!isActive || candidates.isEmpty()) {
                _scanStats.value = _scanStats.value.copy(scanning = false)
                return@launch
            }

            // Deep Test
            _scanStats.value = _scanStats.value.copy(isSecondPass = true)
            val deepTestJobs = candidates.map { candidate ->
                async {
                    if (!isActive) return@async
                    var successes = 0
                    var minLatency = Long.MAX_VALUE
                    
                    repeat(3) {
                        _scanStats.value = _scanStats.value.copy(
                            totalScanned = _scanStats.value.totalScanned + 1,
                            currentIP = candidate.ip,
                            currentPort = candidate.port
                        )
                        val start = System.currentTimeMillis()
                        val ok = if (tlsPorts.contains(candidate.port)) {
                            checkTlsConnection(candidate.ip, candidate.port, 3000)
                        } else {
                            checkTcpConnection(candidate.ip, candidate.port, 3000) >= 0
                        }
                        
                        if (ok) {
                            successes++
                            val lat = System.currentTimeMillis() - start
                            if (lat < minLatency) minLatency = lat
                        }
                    }

                    if (successes >= 2) {
                        val finalResult = candidate.copy(latencyMs = minLatency)
                        _scanResults.emit(finalResult)
                    }
                }
            }
            deepTestJobs.awaitAll()
            _scanStats.value = _scanStats.value.copy(scanning = false)
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _scanStats.value = _scanStats.value.copy(scanning = false)
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

    private fun checkTlsConnection(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            val sslSocket = factory.createSocket(socket, "nova2.altramax083.workers.dev", port, true) as SSLSocket
            sslSocket.soTimeout = timeout
            sslSocket.startHandshake()
            sslSocket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateRandomIPs(cidr: String, count: Int): List<String> {
        // Simple mock for random IP generation from CIDR
        // Real implementation would parse CIDR properly
        val parts = cidr.split("/")
        val ipParts = parts[0].split(".")
        val results = mutableListOf<String>()
        repeat(count) {
            val last = (1..254).random()
            results.add("${ipParts[0]}.${ipParts[1]}.${ipParts[2]}.$last")
        }
        return results.distinct()
    }
}
